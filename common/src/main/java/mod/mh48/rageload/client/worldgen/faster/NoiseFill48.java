package mod.mh48.rageload.client.worldgen.faster;

import com.google.common.collect.Sets;
import mod.mh48.rageload.RageLoad;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


public class NoiseFill48 {


    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public static CompletableFuture<ChunkAccess> fillFromNoise(
            NoiseBasedChunkGenerator gen, Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess
    ) {
        NoiseSettings noiseSettings = gen.generatorSettings().value().noiseSettings().clampToHeightAccessor(chunkAccess.getHeightAccessorForGeneration());
        int yCellHeight = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());


        int minY = noiseSettings.minY();
        int maxY = minY+chunkAccess.getHeightAccessorForGeneration().getHeight();

        if (yCellHeight <= 0) {
            return CompletableFuture.completedFuture(chunkAccess);
        } else {
            int l = chunkAccess.getSectionIndex(yCellHeight * noiseSettings.getCellHeight() - 1 + minY);
            int m = chunkAccess.getSectionIndex(minY);
            Set<LevelChunkSection> set = Sets.<LevelChunkSection>newHashSet();

            for (int n = l; n >= m; n--) {
                LevelChunkSection levelChunkSection = chunkAccess.getSection(n);
                levelChunkSection.acquire();
                set.add(levelChunkSection);
            }

            return CompletableFuture.supplyAsync(
                            Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
                                try{
                                    return doFill(gen,blender, structureManager, randomState, chunkAccess, minY, maxY);
                                }catch (Exception exception){
                                    RageLoad.LOGGER.error("Error during fill noise", exception);
                                    return chunkAccess;
                                }

                            }),
                            executor
                    )
                    .whenCompleteAsync((chunkAccessx, throwable) -> {
                        for (LevelChunkSection levelChunkSectionx : set) {
                            levelChunkSectionx.release();
                        }
                    }, executor);
        }
    }



    private static ChunkAccess doFill(NoiseBasedChunkGenerator gen,Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess, int minY, int maxY) {
        //NoiseChunk noiseChunk = chunkAccess.getOrCreateNoiseChunk(chunkAccessx -> createNoiseChunk(gen,chunkAccessx, structureManager, blender, randomState));
        Heightmap heightmapOcean = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmapSurface = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunkAccess.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        NoiseSettings noiseSettings = gen.generatorSettings().value().noiseSettings().clampToHeightAccessor(chunkAccess);

        MutableSinglePointContext singlePointContext = new MutableSinglePointContext();

        SimpleDFCaches.WrapData wrapData = new SimpleDFCaches.WrapData(noiseSettings.getCellWidth(),noiseSettings.getCellHeight(),noiseSettings.height(),noiseSettings.minY());

        DensityFunction finalDensity = SimpleDFCaches.wrap(randomState.router().finalDensity(),wrapData);

        DensityFunction initDensity = SimpleDFCaches.wrap(randomState.router().initialDensityWithoutJaggedness(),wrapData);


        NoiseChunk noiseChunk = chunkAccess.getOrCreateNoiseChunk(chunkAccessx -> FakeNoiseChunk.init(noiseSettings,initDensity,randomState,gen,chunkPos));
        Aquifer aquifer = noiseChunk.aquifer();//
        //new Aquifer48(chunkPos,randomState.router(),randomState.aquiferRandom(), noiseSettings.minY(), noiseSettings.height(), fluidPicker, noiseChunk::preliminarySurfaceLevel);

        BlockState defaultBlock = gen.generatorSettings().value().defaultBlock();

        //noiseChunk.updateForY(yBlock, cellYProgress);
        for (int xBlock = minX; xBlock <= maxX; xBlock++) {
            int xSection = xBlock & 15;
            //noiseChunk.updateForX(xBlock, cellXProgress);
            for (int zBlock = minZ; zBlock <= maxZ; zBlock++) {
                int zSection = zBlock & 15;

                int yj = maxY;
                while (yj > minY) {
                    yj--;
                    singlePointContext.set(xBlock, yj, zBlock);
                    BlockState blockState = aquifer.computeSubstance(singlePointContext,finalDensity.compute(singlePointContext));
                    if (blockState == null) {
                        blockState = defaultBlock;
                    }
                    int ySection = yj & 15;
                    int cellSectionIndex = chunkAccess.getSectionIndex(yj);
                    LevelChunkSection levelChunkSection = chunkAccess.getSection(cellSectionIndex);
                    levelChunkSection.setBlockState(xSection, ySection, zSection, blockState, false);
                    heightmapOcean.update(xSection, yj, zSection, blockState);
                    heightmapSurface.update(xSection, yj, zSection, blockState);
                }
            }
        }

        return chunkAccess;
    }


    private static ChunkAccess doFill2d(NoiseBasedChunkGenerator gen,Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess, int minY, int maxY) {
        //NoiseChunk noiseChunk = chunkAccess.getOrCreateNoiseChunk(chunkAccessx -> createNoiseChunk(gen,chunkAccessx, structureManager, blender, randomState));
        Heightmap heightmapOcean = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmapSurface = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunkAccess.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        NoiseSettings noiseSettings = gen.generatorSettings().value().noiseSettings().clampToHeightAccessor(chunkAccess);

        MutableSinglePointContext singlePointContext = new MutableSinglePointContext();

        SimpleDFCaches.WrapData wrapData = new SimpleDFCaches.WrapData(noiseSettings.getCellWidth(),noiseSettings.getCellHeight(),noiseSettings.height(),noiseSettings.minY());

        DensityFunction finalDensity = SimpleDFCaches.wrap(randomState.router().finalDensity(),wrapData);

        DensityFunction initDensity = SimpleDFCaches.wrap(randomState.router().initialDensityWithoutJaggedness(),wrapData);


        NoiseChunk noiseChunk = chunkAccess.getOrCreateNoiseChunk(chunkAccessx -> FakeNoiseChunk.init(noiseSettings,initDensity,randomState,gen,chunkPos));
        Aquifer aquifer = noiseChunk.aquifer();//
        //new Aquifer48(chunkPos,randomState.router(),randomState.aquiferRandom(), noiseSettings.minY(), noiseSettings.height(), fluidPicker, noiseChunk::preliminarySurfaceLevel);

        BlockState defaultBlock = gen.generatorSettings().value().defaultBlock();

        //noiseChunk.updateForY(yBlock, cellYProgress);
        for (int xBlock = minX; xBlock <= maxX; xBlock++) {
            int xSection = xBlock & 15;
            //noiseChunk.updateForX(xBlock, cellXProgress);
            for (int zBlock = minZ; zBlock <= maxZ; zBlock++) {
                int zSection = zBlock & 15;



                int yJump = maxY-minY;
                int yj = maxY;
                while (yJump > 0) {
                    yJump = yJump/2;
                    singlePointContext.set(xBlock, yj-yJump, zBlock);
                    if(initDensity.compute(singlePointContext)<0){
                        yj = yj-yJump;
                    }
                }

                BlockState blockState = AIR;

                while (yj > minY && blockState!=defaultBlock) {
                    yj--;
                    singlePointContext.set(xBlock, yj, zBlock);
                    blockState = aquifer.computeSubstance(singlePointContext,finalDensity.compute(singlePointContext));
                    if (blockState == null) {
                        blockState = defaultBlock;
                    }
                    int ySection = yj & 15;
                    int cellSectionIndex = chunkAccess.getSectionIndex(yj);
                    LevelChunkSection levelChunkSection = chunkAccess.getSection(cellSectionIndex);
                    levelChunkSection.setBlockState(xSection, ySection, zSection, blockState, false);
                    heightmapOcean.update(xSection, yj, zSection, blockState);
                    heightmapSurface.update(xSection, yj, zSection, blockState);
                }

                int yfillstart = yj;

                while (yj < maxY && blockState!=AIR) {//todo Overlap
                    yj++;
                    singlePointContext.set(xBlock, yj, zBlock);
                    blockState = aquifer.computeSubstance(singlePointContext,finalDensity.compute(singlePointContext));
                    if (blockState == null) {
                        blockState = defaultBlock;
                    }

                    int ySection = yj & 15;
                    int cellSectionIndex = chunkAccess.getSectionIndex(yj);
                    LevelChunkSection levelChunkSection = chunkAccess.getSection(cellSectionIndex);
                    levelChunkSection.setBlockState(xSection, ySection, zSection, blockState, false);
                    heightmapOcean.update(xSection, yj, zSection, blockState);
                    heightmapSurface.update(xSection, yj, zSection, blockState);
                }

                blockState = gen.generatorSettings().value().defaultBlock();

                while (yfillstart > minY) {
                    int ySection = yfillstart & 15;
                    int cellSectionIndex = chunkAccess.getSectionIndex(yfillstart);
                    LevelChunkSection levelChunkSection = chunkAccess.getSection(cellSectionIndex);
                    levelChunkSection.setBlockState(xSection, ySection, zSection, blockState, false);
                    heightmapOcean.update(xSection, yfillstart, zSection, blockState);
                    heightmapSurface.update(xSection, yfillstart, zSection, blockState);
                    yfillstart--;
                }
            }

        }



        return chunkAccess;
    }

}
