package mod.mh48.rageload.client.worldgen;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SaveProtoChunk extends ProtoChunk {

    private final LevelChunk wrapped;
    private final Holder<Biome> defaultBiome;

    public SaveProtoChunk(LevelChunk levelChunk, Registry<Biome> biomeRegistry) {
        super(levelChunk.getPos(), UpgradeData.EMPTY, levelChunk.getLevel() , biomeRegistry, null);
        this.wrapped = levelChunk;
        for (int i = 0; i < this.getSections().length; i++) {
            this.getSections()[i] = new SaveLevelChunkSection(biomeRegistry);
        }
        defaultBiome = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);
    }


    @Nullable
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        try {
            return this.wrapped.getBlockEntity(blockPos);
        }catch (Exception e){
            return null;
        }
    }

    public BlockState getBlockState(BlockPos blockPos) {
        try {//This is always unsave because threading. But writes are rare so I don't realy care that much.
            return this.wrapped.getBlockState(blockPos);
        }catch (Exception e){
            return Blocks.VOID_AIR.defaultBlockState();
        }
    }

    public FluidState getFluidState(BlockPos blockPos) {
        return this.getBlockState(blockPos).getFluidState();
    }

    public int getMaxLightLevel() {
        return this.wrapped.getMaxLightLevel();
    }

    public LevelChunkSection getSection(int i) {
        return super.getSection(i);
    }

    @Nullable
    public BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean bl) {
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        if (j >= this.getMinBuildHeight() && j < this.getMaxBuildHeight()) {
            int l = this.getSectionIndex(j);
            LevelChunkSection levelChunkSection = this.getSection(l);
            boolean bl2 = levelChunkSection.hasOnlyAir();
            if (bl2 && blockState.is(Blocks.AIR)) {
                return blockState;
            } else {
                int m = SectionPos.sectionRelative(i);
                int n = SectionPos.sectionRelative(j);
                int o = SectionPos.sectionRelative(k);

                return levelChunkSection.setBlockState(m, n, o, blockState);
            }
        } else {
            return Blocks.VOID_AIR.defaultBlockState();
        }
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        super.setBlockEntity(blockEntity);
    }

    public void addEntity(Entity entity) {
    }

    public void setStatus(ChunkStatus chunkStatus) {
        super.setStatus(chunkStatus);
    }

    public LevelChunkSection[] getSections() {
        return super.getSections();
    }

    public class SaveLevelChunkSection extends LevelChunkSection{
        public SaveLevelChunkSection(Registry<Biome> biomeRegistry) {
            super(biomeRegistry);
        }

        public ReentrantLock lock = new ReentrantLock();

        public void acquire() {
            lock.lock();
        }

        public boolean tryAcquire() {
            return lock.tryLock();
        }

        public void release() {
            lock.unlock();
        }
    }

    public void merge(){//Only run in client thread
        int chunkWorldX = this.wrapped.getPos().getMinBlockX();
        int chunkWorldZ = this.wrapped.getPos().getMinBlockZ();
        for (int i = 0; i < this.getSections().length; i++) {
            SaveLevelChunkSection section = (SaveLevelChunkSection) this.getSections()[i];
            if(section.hasOnlyAir())continue;
            LevelChunkSection wsection = wrapped.getSections()[i];
            if(!section.tryAcquire())continue;
            wsection.acquire();
            int sectionWorldY = this.wrapped.getSectionYFromSectionIndex(i) << 4;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.setBlockState(x, y, z, Blocks.AIR.defaultBlockState(), false);
                        if(state.isAir())continue;
                        BlockState old = wsection.setBlockState(x, y, z, state, false);
                        if (state != old) {
                            mutablePos.set(chunkWorldX + x, sectionWorldY + y, chunkWorldZ + z);
                            wrapped.getLevel().sendBlockUpdated(mutablePos, old, state, 2);
                        }
                    }
                }
            }
            section.release();
            wsection.release();
        }
    }

    public void setHeightmap(Heightmap.Types types, long[] ls) {
    }

    private Heightmap.Types fixType(Heightmap.Types types) {
        if (types == Heightmap.Types.WORLD_SURFACE_WG) {
            return Heightmap.Types.WORLD_SURFACE;
        } else {
            return types == Heightmap.Types.OCEAN_FLOOR_WG ? Heightmap.Types.OCEAN_FLOOR : types;
        }
    }

    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types types) {
        return this.wrapped.getOrCreateHeightmapUnprimed(types);
    }

    public int getHeight(Heightmap.Types types, int i, int j) {
        return this.wrapped.getHeight(this.fixType(types), i, j);
    }

    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        try {//This is always unsave because threading. But writes are rare so I don't realy care that much.
            return this.wrapped.getNoiseBiome(i, j, k);
        }catch (Exception e){
            return this.defaultBiome;
        }

    }

    public ChunkPos getPos() {
        return this.wrapped.getPos();
    }

    @Nullable
    public StructureStart getStartForStructure(Structure structure) {
        return super.getStartForStructure(structure);
    }

    public void setStartForStructure(Structure structure, StructureStart structureStart) {
    }

    public Map<Structure, StructureStart> getAllStarts() {
        return super.getAllStarts();
    }

    public void setAllStarts(Map<Structure, StructureStart> map) {
    }

    public LongSet getReferencesForStructure(Structure structure) {
        return super.getReferencesForStructure(structure);
    }

    public void addReferenceForStructure(Structure structure, long l) {
    }

    public Map<Structure, LongSet> getAllReferences() {
        return super.getAllReferences();
    }

    public void setAllReferences(Map<Structure, LongSet> map) {
    }

    public void setUnsaved(boolean bl) {
        super.setUnsaved(bl);
    }

    public boolean isUnsaved() {
        return false;
    }

    public ChunkStatus getStatus() {
        return super.getStatus();
    }

    public void removeBlockEntity(BlockPos blockPos) {
    }

    public void markPosForPostprocessing(BlockPos blockPos) {
    }

    public void setBlockEntityNbt(CompoundTag compoundTag) {
    }

    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos blockPos) {
        return this.wrapped.getBlockEntityNbt(blockPos);
    }

    @Nullable
    public CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos) {
        return this.wrapped.getBlockEntityNbtForSaving(blockPos);
    }

    public void findBlocks(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> biConsumer) {
        this.wrapped.findBlocks(predicate, biConsumer);
    }

    public TickContainerAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyContainer();
    }

    public TickContainerAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyContainer();
    }

    public TicksToSave getTicksForSerialization() {
        return this.wrapped.getTicksForSerialization();
    }

    @Nullable
    public BlendingData getBlendingData() {
        return this.wrapped.getBlendingData();
    }

    public void setBlendingData(BlendingData blendingData) {
        //this.wrapped.setBlendingData(blendingData);
    }

    public CarvingMask getCarvingMask(GenerationStep.Carving carving) {
        throw new UnsupportedOperationException();
    }

    public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving carving) {
        throw new UnsupportedOperationException();
    }

    public LevelChunk getWrapped() {
        return this.wrapped;
    }

    public boolean isLightCorrect() {
        return true;
    }

    public void setLightCorrect(boolean bl) {
    }

    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler) {
        throw new UnsupportedOperationException();
    }

    public void initializeLightSources() {
    }

    public ChunkSkyLightSources getSkyLightSources() {
        throw new UnsupportedOperationException();
    }
}
