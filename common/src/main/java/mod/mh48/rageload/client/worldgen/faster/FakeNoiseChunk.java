package mod.mh48.rageload.client.worldgen.faster;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import mod.mh48.rageload.RageLoad;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FakeNoiseChunk extends NoiseChunk{


    public Long2IntOpenHashMap preliminarySurfaceLevel;
    public NoiseSettings noiseSettings;
    public DensityFunction initialDensityNoJaggedness;

    public Aquifer aquifer;

    public static FakeNoiseChunk init(NoiseSettings noiseSettings, DensityFunction initialDensityNoJaggedness, RandomState randomState, NoiseBasedChunkGenerator gen, ChunkPos pos){
        try {
            FakeNoiseChunk fns = (FakeNoiseChunk) RageLoad.unsafe.allocateInstance(FakeNoiseChunk.class);
            fns.noiseSettings = noiseSettings;
            fns.initialDensityNoJaggedness = initialDensityNoJaggedness;
            fns.preliminarySurfaceLevel = new Long2IntOpenHashMap();
            Aquifer.FluidPicker fluidPicker = createFluidPicker(gen.generatorSettings().value());
            fns.aquifer = Aquifer.create(fns, pos, randomState.router(), randomState.aquiferRandom(), noiseSettings.minY(), noiseSettings.height(), fluidPicker);

            return fns;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings noiseGeneratorSettings) {
        Aquifer.FluidStatus fluidStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = noiseGeneratorSettings.seaLevel();
        Aquifer.FluidStatus fluidStatus2 = new Aquifer.FluidStatus(i, noiseGeneratorSettings.defaultFluid());
        Aquifer.FluidStatus fluidStatus3 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
        return (j, k, l) -> k < Math.min(-54, i) ? fluidStatus : fluidStatus2;
    }

    private FakeNoiseChunk() {
        super(0,null,0,0,null,null,null,null,null);
        throw new RuntimeException("You managed to do the impossible. Be proud of yourself");
    }

    @Override
    public Climate.Sampler cachedClimateSampler(NoiseRouter noiseRouter, List<Climate.ParameterPoint> list) {
        return new Climate.Sampler(
                noiseRouter.temperature(),
                noiseRouter.vegetation(),
                noiseRouter.continents(),
                noiseRouter.erosion(),
                noiseRouter.depth(),
                noiseRouter.ridges(),
                list
        );
    }

    @Override
    public @Nullable BlockState getInterpolatedState() {
        return null;
    }

    @Override
    public int blockX() {
        return 0;
    }

    @Override
    public int blockY() {
        return 0;
    }

    @Override
    public int blockZ() {
        return 0;
    }

    @Override
    public int preliminarySurfaceLevel(int i, int j) {
        int k = QuartPos.toBlock(QuartPos.fromBlock(i));
        int l = QuartPos.toBlock(QuartPos.fromBlock(j));
        return this.preliminarySurfaceLevel.computeIfAbsent(ColumnPos.asLong(k, l), this::computePreliminarySurfaceLevel);
    }

    private int computePreliminarySurfaceLevel(long l) {
        int x = ColumnPos.getX(l);
        int z = ColumnPos.getZ(l);
        int minY = this.noiseSettings.minY();

        for (int y = minY + this.noiseSettings.height(); y >= minY; y -= noiseSettings.getCellHeight()) {
            if (this.initialDensityNoJaggedness.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.390625) {
                return y;
            }
        }

        return Integer.MAX_VALUE;
    }

    public static int clamp(long value, int min, int max) {
        return (int) Math.min(max, Math.max(value, min));
    }

    @Override
    public Blender getBlender() {
        return Blender.empty();
    }

    @Override
    public void initializeForFirstCellX() {
    }

    @Override
    public void advanceCellX(int i) {
    }

    @Override
    public NoiseChunk forIndex(int i) {
        return this;
    }

    @Override
    public void fillAllDirectly(double[] ds, DensityFunction densityFunction) {
    }

    @Override
    public void selectCellYZ(int i, int j) {
    }

    @Override
    public void updateForY(int i, double d) {
    }

    @Override
    public void updateForX(int i, double d) {
    }

    @Override
    public void updateForZ(int i, double d) {
    }

    @Override
    public void stopInterpolation() {
    }

    @Override
    public void swapSlices() {
    }

    @Override
    public Aquifer aquifer() {
        return aquifer;
    }

    @Override
    public int cellWidth() {
        return noiseSettings.getCellWidth();
    }

    @Override
    public int cellHeight() {
        return noiseSettings.getCellHeight();
    }

    @Override
    public DensityFunction wrap(DensityFunction densityFunction) {
        return densityFunction;
    }
}
