package mod.mh48.rageload.client.worldgen.faster;

import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.Arrays;
import java.util.HashMap;

public class SimpleDFCaches {

    private static final ThreadLocal<HashMap<DensityFunction,DensityFunction>> wrappedDFs = ThreadLocal.withInitial(HashMap::new);


    public record WrapData(int cellWidth,int cellHeight,int height,int minY){
    }

    public static Climate.Sampler wrap(Climate.Sampler sampler, WrapData wrapData) {
        return new Climate.Sampler(
                wrap(sampler.temperature(), wrapData),
                wrap(sampler.humidity(), wrapData),
                wrap(sampler.continentalness(), wrapData),
                wrap(sampler.erosion(), wrapData),
                wrap(sampler.depth(), wrapData),
                wrap(sampler.weirdness(), wrapData),
                sampler.spawnTarget()
        );
    }

    public static DensityFunction wrap(DensityFunction densityFunction, WrapData pWrapData) {
        HashMap<DensityFunction, DensityFunction> wdfs = wrappedDFs.get();
        if(wdfs.containsKey(densityFunction)){
            return wdfs.get(densityFunction);
        }
        DensityFunction wrapped = densityFunction.mapAll((df)->wrap_(df,pWrapData));
        wdfs.put(densityFunction,wrapped);
        return wrapped;
    }

    private static DensityFunction wrap_(DensityFunction densityFunction, WrapData pWrapData) {
        return wrappedDFs.get().computeIfAbsent(densityFunction, (densityFunction1) -> wrapNew(densityFunction1, pWrapData));
    }

    private static DensityFunction wrapNew(DensityFunction densityFunction,WrapData pWrapData) {
        if (densityFunction instanceof DensityFunctions.Marker marker) {
            return switch (marker.type()) {
                case Interpolated -> new NoiseInterpolator(marker.wrapped(),pWrapData);
                case FlatCache -> new FlatCache(marker.wrapped());
                case Cache2D -> new Cache2D(marker.wrapped());
                case CacheOnce -> new CacheOnce(marker.wrapped());
                //case CacheAllInCell -> new NoiseChunk.CacheAllInCell(marker.wrapped());
                default -> densityFunction;
            };
        }
        return densityFunction;
    }

    public static class Cache2D implements DensityFunctions.MarkerOrMarked {
        private final DensityFunction function;
        private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        public Cache2D(DensityFunction densityFunction) {
            this.function = densityFunction;
        }

        @Override
        public double compute(FunctionContext functionContext) {
            int i = functionContext.blockX();
            int j = functionContext.blockZ();
            long l = ChunkPos.asLong(i, j);
            if (this.lastPos2D == l) {
                return this.lastValue;
            } else {
                this.lastPos2D = l;
                double d = this.function.compute(functionContext);
                this.lastValue = d;
                return d;
            }
        }

        @Override
        public void fillArray(double[] ds, ContextProvider contextProvider) {
            this.function.fillArray(ds, contextProvider);
        }

        @Override
        public double minValue() {
            return function.minValue();
        }

        @Override
        public double maxValue() {
            return function.maxValue();
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Cache2D;
        }
    }

    public static class CacheOnce implements DensityFunctions.MarkerOrMarked {
        private final DensityFunction function;
        private double lastValue;
        private int lastX;
        private int lastY;
        private int lastZ;

        public CacheOnce(DensityFunction densityFunction) {
            this.function = densityFunction;
        }

        @Override
        public double compute(FunctionContext functionContext) {
            if(functionContext.blockX() == lastX && functionContext.blockZ() == lastZ && functionContext.blockY() == lastY) {
                return this.lastValue;
            }
            double d = this.function.compute(functionContext);
            this.lastValue = d;
            this.lastX = functionContext.blockX();
            this.lastY = functionContext.blockY();
            this.lastZ = functionContext.blockZ();
            return d;
        }

        @Override
        public void fillArray(double[] ds, ContextProvider contextProvider) {
            this.wrapped().fillArray(ds, contextProvider);
        }

        @Override
        public double minValue() {
            return function.minValue();
        }

        @Override
        public double maxValue() {
            return function.maxValue();
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheOnce;
        }
    }

    public static class FlatCache implements DensityFunctions.MarkerOrMarked {
        private final DensityFunction noiseFiller;
        private final MutableSinglePointContext mutableSinglePointContext = new MutableSinglePointContext();

        private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        FlatCache(DensityFunction densityFunction) {
            this.noiseFiller = densityFunction;
        }

        @Override
        public double compute(FunctionContext functionContext) {
            int i = QuartPos.fromBlock(functionContext.blockX());
            int j = QuartPos.fromBlock(functionContext.blockZ());
            long l = ChunkPos.asLong(i, j);
            if (this.lastPos2D == l) {
                return this.lastValue;
            } else {
                this.lastPos2D = l;
                int x = QuartPos.toBlock(i);
                int z = QuartPos.toBlock(j);
                mutableSinglePointContext.set(x,0,z);
                double d = this.noiseFiller.compute(mutableSinglePointContext);
                this.lastValue = d;
                return d;
            }
        }

        @Override
        public void fillArray(double[] ds, ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public double minValue() {
            return noiseFiller.minValue();
        }

        @Override
        public double maxValue() {
            return noiseFiller.maxValue();
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.FlatCache;
        }
    }


    public static class NoiseInterpolator implements DensityFunctions.MarkerOrMarked {
        private final DensityFunction noiseFiller;
        private final WrapData wrapData;
        private long chunkPos = ChunkPos.INVALID_CHUNK_POS;
        private double[] values;
        private int sizeXZ;          // Anzahl Zellen in X- und Z-Richtung
        private int sizeY;           // Anzahl Zellen in Y-Richtung
        private int chunkStartX;     // Block-X-Start des aktuellen Chunks
        private int chunkStartZ;     // Block-Z-Start des aktuellen Chunks
        private final MutableSinglePointContext mutableSinglePointContext = new MutableSinglePointContext();

        private NoiseInterpolator(DensityFunction densityFunction, WrapData wrapData) {
            this.noiseFiller = densityFunction;
            this.wrapData = wrapData;
            this.sizeXZ = 16 / wrapData.cellWidth + 1;      // 16 = Chunkbreite
            this.sizeY = wrapData.height / wrapData.cellHeight + 1;
            this.values = new double[sizeXZ * sizeXZ * sizeY];
            resetValues();
        }

        public void resetValues() {
            // Alle Werte auf NaN setzen, damit sie bei Bedarf neu berechnet werden
            Arrays.fill(this.values, Double.NaN);
        }

        @Override
        public double compute(FunctionContext functionContext) {
            // Aktuellen Chunk aus Blockkoordinaten bestimmen
            long chunk = ChunkPos.asLong(
                    SectionPos.blockToSectionCoord(functionContext.blockX()),
                    SectionPos.blockToSectionCoord(functionContext.blockZ())
            );
            if (chunk != this.chunkPos) {
                this.chunkPos = chunk;
                this.chunkStartX = ChunkPos.getX(chunk) * 16;
                this.chunkStartZ = ChunkPos.getZ(chunk) * 16;
                resetValues();
            }

            // Zellkoordinaten der unteren linken Ecke der aktuellen Zelle
            int baseCellX = Math.floorDiv((functionContext.blockX() - chunkStartX) , wrapData.cellWidth);
            int baseCellY = Math.floorDiv((functionContext.blockY() - wrapData.minY) , wrapData.cellHeight);
            int baseCellZ = Math.floorDiv((functionContext.blockZ() - chunkStartZ) , wrapData.cellWidth);

            // Nachbarzellen klemmen, damit wir nicht außerhalb des Chunks zugreifen
            int nextCellX = baseCellX + 1;
            int nextCellY = baseCellY + 1;
            int nextCellZ = baseCellZ + 1;

            // 8 Eckwerte über getValue holen
            double v000 = getValue(baseCellX, baseCellY, baseCellZ);
            double v100 = getValue(nextCellX, baseCellY, baseCellZ);
            double v010 = getValue(baseCellX, nextCellY, baseCellZ);
            double v110 = getValue(nextCellX, nextCellY, baseCellZ);
            double v001 = getValue(baseCellX, baseCellY, nextCellZ);
            double v101 = getValue(nextCellX, baseCellY, nextCellZ);
            double v011 = getValue(baseCellX, nextCellY, nextCellZ);
            double v111 = getValue(nextCellX, nextCellY, nextCellZ);

            // Interpolationsfaktoren (0..1) aus der Position innerhalb der Zelle
            double xFrac = (functionContext.blockX() - chunkStartX - baseCellX * wrapData.cellWidth) / (double) wrapData.cellWidth;
            double yFrac = (functionContext.blockY() - wrapData.minY - baseCellY * wrapData.cellHeight) / (double) wrapData.cellHeight;
            double zFrac = (functionContext.blockZ() - chunkStartZ - baseCellZ * wrapData.cellWidth) / (double) wrapData.cellWidth;

            // Trilineare Interpolation durchführen
            return Mth.lerp3(
                    xFrac, yFrac, zFrac,
                    v000, v100, v010, v110,
                    v001, v101, v011, v111
            );
        }

        public double getValue(int cellX, int cellY, int cellZ) {
            // Index im 1D-Array berechnen (X ist äußerste Dimension, dann Z, dann Y)
            int index = (cellX * sizeXZ + cellZ) * sizeY + cellY;

            // Nur neu berechnen, wenn noch nicht geschehen
            if (Double.isNaN(values[index])) {
                // Zellkoordinaten in Blockkoordinaten umrechnen
                int blockX = chunkStartX + cellX * wrapData.cellWidth;
                int blockY = wrapData.minY + cellY * wrapData.cellHeight;
                int blockZ = chunkStartZ + cellZ * wrapData.cellWidth;

                mutableSinglePointContext.set(blockX, blockY, blockZ);
                values[index] = this.noiseFiller.compute(mutableSinglePointContext);
            }
            return values[index];
        }

        @Override
        public void fillArray(double[] ds, ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public double minValue() {
            return noiseFiller.minValue();
        }

        @Override
        public double maxValue() {
            return noiseFiller.maxValue();
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Interpolated;
        }
    }
}
