package mod.mh48.rageload.mixin.debug;

import mod.mh48.rageload.duck.debug.ProtoChunkStatsDuck;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.LongAdder;

import static mod.mh48.rageload.duck.debug.ProtoChunkStats.SAMPLES;
import static mod.mh48.rageload.duck.debug.ProtoChunkStats.TOTAL_TIME;

@Mixin(ProtoChunk.class)
public class ProtoChunkClientPerformanceLoggerMixin implements ProtoChunkStatsDuck {

    @Unique
    private long atria48$startTime;

    @Unique
    private ChunkStatus atria48$status;

    @Inject(method = "setStatus", at = @At("HEAD"))
    private void captureAverage(ChunkStatus nextStatus, CallbackInfo ci) {
        if(atria48$status==null)return;
        long now = System.nanoTime();

        long duration = now - this.atria48$startTime;
        TOTAL_TIME.computeIfAbsent(atria48$status, k -> new LongAdder()).add(duration);
        SAMPLES.computeIfAbsent(atria48$status, k -> new LongAdder()).increment();
        atria48$status = null;
    }

    @Unique
    @Override
    public void atrioffload$startStatus(ChunkStatus status){
        atria48$status = status;
        atria48$startTime = System.nanoTime();
    }
}
