package mod.mh48.rageload.client.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;

public class AdvancedLightRegion {
    public final DataLayer[][] skyData;   // [0-8][SectionY]
    public final DataLayer[][] blockData; // [0-8][SectionY]
    public final WorldGenRegion region;

    public final int[][] skyLightHeight;
    public final int[][] blockStartHeight;
    public final int minBlockX;
    public final int minBlockZ;

    private final int minSectionY;
    private final int maxSectionY;
    private final ChunkPos center;

    // Separate Queues für Sky und Block
    private long[] skyQueue = new long[131072];
    private int skyHead = 0, skyTail = 0;

    private long[] blockQueue = new long[131072];
    private int blockHead = 0, blockTail = 0;

    public AdvancedLightRegion(WorldGenRegion region) {
        this.region = region;
        this.center = region.getCenter();
        this.minSectionY = region.getLightEngine().getMinLightSection();
        this.maxSectionY = minSectionY + region.getLightEngine().getLightSectionCount() - 1;

        int sections = region.getLightEngine().getLightSectionCount();
        this.skyData = new DataLayer[9][sections];
        this.blockData = new DataLayer[9][sections];
        this.skyLightHeight = new int[48][48];
        this.blockStartHeight = new int[48][48];

        this.minBlockX = center.getMinBlockX() - 16;
        this.minBlockZ = center.getMinBlockZ() - 16;
    }

    public void initSkyLight(int x, int z) {
        ChunkAccess chunk = region.getChunk(x, z);
        ChunkPos center = region.getCenter();
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        int maxY = (maxSectionY + 1) * 16 - 1;
        int minY = minSectionY * 16;

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        // 1. SKYLIGHT: Heightmap Drop
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int surfaceY = chunk.getMinBuildHeight();
                int blockStart = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) + 1;
                for (int y = blockStart; y >= chunk.getMinBuildHeight(); y--) {
                    mPos.set(worldX, y, worldZ);
                    BlockState state = region.getBlockState(mPos);
                    if (!state.propagatesSkylightDown(region, mPos)) {
                        surfaceY = y;
                        break;
                    }
                }

                blockStartHeight[(x - center.x + 1) * 16 + localX][(z - center.z + 1) * 16 + localZ] = blockStart + 1;
                skyLightHeight[(x - center.x + 1) * 16 + localX][(z - center.z + 1) * 16 + localZ] = surfaceY;
            }
        }
    }

    public void finalizeLight() {
        // Flood-Fill für SkyLight
        runFloodFill(true);

        // Flood-Fill für BlockLight
        runFloodFill(false);
    }

    public void initQueuesFrom3x3() {
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        // Scanne das gesamte 3x3 Grid, ABER...
        for (int cx = center.x - 1; cx <= center.x + 1; cx++) {
            for (int cz = center.z - 1; cz <= center.z + 1; cz++) {
                int minX = cx * 16;
                int minZ = cz * 16;
                ChunkAccess chunk = region.getChunk(cx, cz);
                for (int sectionY = region.getMinSection(); sectionY < region.getMaxSection(); sectionY++) {
                    if (chunk.getSection(sectionY - region.getMinSection()).maybeHas(state -> state.getLightEmission() > 0)) {
                        for (int lx = 0; lx < 16; lx++) {
                            for (int lz = 0; lz < 16; lz++) {
                                for (int ly = 0; ly < 16; ly++) {
                                    int worldX = minX + lx;
                                    int worldY = (sectionY << 4) + ly;
                                    int worldZ = minZ + lz;
                                    mPos.set(worldX, worldY, worldZ);
                                    BlockState state = chunk.getBlockState(mPos);
                                    int emission = state.getLightEmission();
                                    if (emission > 0) {
                                        setLight(blockData, mPos.getX(), worldY, mPos.getZ(), emission);
                                        enqueueBlock(worldX, worldY, worldZ);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int lx = 0; lx < 48; lx++) {
            for (int lz = 0; lz < 48; lz++) {
                int worldX = minBlockX + lx;
                int worldZ = minBlockZ + lz;
                int sy = this.skyLightHeight[lx][lz];
                int syAround = sy;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (lx + x > 0 && lx + x < 48 && lz + z > 0 && lz + z < 48) {
                            int syT = this.blockStartHeight[lx + x][lz + z];
                            if (syT > syAround) {
                                syAround = syT;
                            }
                        }
                    }
                }

                for (int y = syAround; y >= sy; y--) {
                    setLight(skyData, worldX, y, worldZ, 15);
                    enqueueSky(worldX, y, worldZ);
                }
            }
        }
    }

    private void runFloodFill(boolean isSkyLight) {
        Direction[] ds = Direction.values();


        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int head;
        DataLayer[][] dataSet;
        long[] queue;
        if (isSkyLight) {
            head = this.skyHead;
            dataSet = this.skyData;
            queue = this.skyQueue;
        } else {
            head = this.blockHead;
            dataSet = this.blockData;
            queue = this.blockQueue;
        }

        while (head < (isSkyLight ? this.skyTail : this.blockTail)) {
            long packed = queue[head++];
            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);

            int currentLight = getLight(dataSet, x, y, z);
            if (currentLight <= 0) continue;

            for (int i = 0; i < ds.length; i++) {
                int nx = x + ds[i].getStepX();
                int ny = y + ds[i].getStepY();
                int nz = z + ds[i].getStepZ();

                if (!isInBounds(nx, ny, nz)) continue;

                pos.set(nx, ny, nz);
                BlockState state = region.getBlockState(pos);
                int opacity = Math.max(1, state.getLightBlock(region, pos));
                int targetLight = currentLight - opacity;

                if (targetLight > getLight(dataSet, nx, ny, nz) && !shapeOccludes(pos, ds[i])) {
                    setLight(dataSet, nx, ny, nz, targetLight);
                    if (isSkyLight) {
                        this.enqueueSky(nx, ny, nz);
                        queue = this.skyQueue;
                    } else {
                        this.enqueueBlock(nx, ny, nz);
                        queue = this.blockQueue;
                    }
                }
            }
        }
    }

    protected boolean shapeOccludes(BlockPos.MutableBlockPos pos, Direction direction) {
        VoxelShape voxelShape2 = LightEngine.getOcclusionShape(region, pos, region.getBlockState(pos), direction.getOpposite());
        pos.move(direction.getOpposite());
        VoxelShape voxelShape = LightEngine.getOcclusionShape(region, pos, region.getBlockState(pos), direction);
        return Shapes.faceShapeOccludes(voxelShape, voxelShape2);
    }

    private void setLight(DataLayer[][] dataSet, int x, int y, int z, int light) {
        DataLayer layer = getOrCreateLayer(dataSet, x, y, z);
        if (layer != null) layer.set(x & 15, y & 15, z & 15, light);
    }

    private int getLight(DataLayer[][] dataSet, int x, int y, int z) {
        if (dataSet == skyData) {
            if (y > skyLightHeight[x - minBlockX][z - minBlockZ]) return 15;
        }
        DataLayer layer = getOrCreateLayer(dataSet, x, y, z);
        return layer != null ? layer.get(x & 15, y & 15, z & 15) : 0;
    }

    private DataLayer getOrCreateLayer(DataLayer[][] dataSet, int x, int y, int z) {
        int chunkX = (x >> 4) - center.x + 1;
        int chunkZ = (z >> 4) - center.z + 1;
        int sectionY = (y >> 4) - minSectionY;

        if (chunkX < 0 || chunkX > 2 || chunkZ < 0 || chunkZ > 2 || sectionY < 0 || sectionY >= dataSet[0].length) {
            return null;
        }

        int chunkIndex = chunkX + chunkZ * 3;
        if (dataSet[chunkIndex][sectionY] == null) {
            dataSet[chunkIndex][sectionY] = new DataLayer();
        }
        return dataSet[chunkIndex][sectionY];
    }

    private boolean isInBounds(int x, int y, int z) {
        int minX = (center.x - 1) << 4;
        int maxX = ((center.x + 2) << 4) - 1;
        int minZ = (center.z - 1) << 4;
        int maxZ = ((center.z + 2) << 4) - 1;
        int minY = minSectionY << 4;
        int maxY = ((maxSectionY + 1) << 4) - 1;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    private void enqueueSky(int x, int y, int z) {
        if (skyTail >= skyQueue.length) skyQueue = Arrays.copyOf(skyQueue, skyQueue.length * 2);
        skyQueue[skyTail++] = packPos(x, y, z);
    }

    private void enqueueBlock(int x, int y, int z) {
        if (blockTail >= blockQueue.length) blockQueue = Arrays.copyOf(blockQueue, blockQueue.length * 2);
        blockQueue[blockTail++] = packPos(x, y, z);
    }

    private long packPos(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    private int unpackX(long packed) {
        return BlockPos.getX(packed);
    }

    private int unpackZ(long packed) {
        return BlockPos.getZ(packed);
    }

    private int unpackY(long packed) {
        return BlockPos.getY(packed);
    }
}
