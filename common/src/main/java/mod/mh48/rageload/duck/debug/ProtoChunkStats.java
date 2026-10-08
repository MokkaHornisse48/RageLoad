package mod.mh48.rageload.duck.debug;

import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class ProtoChunkStats {


    public static final Map<ChunkStatus, LongAdder> TOTAL_TIME = new ConcurrentHashMap<>();

    public static final Map<ChunkStatus, LongAdder> SAMPLES = new ConcurrentHashMap<>();

    public static void logAverages(Consumer<String> consumer) {
        TOTAL_TIME.forEach((status, totalNanos) -> {
            long count = SAMPLES.get(status).sum();
            if (count > 0) {
                double avgMs = (totalNanos.sum() / (double) count) / 1_000_000.0;
                // Use your preferred logger here
                consumer.accept(String.format("Status: %s | Avg: %.4f ms (Samples: %d)",
                        status.toString(), avgMs, count));
            }
        });
    }
}
