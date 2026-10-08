package mod.mh48.rageload.duck.debug;

import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Unique;

public interface ProtoChunkStatsDuck {
    @Unique
    void atrioffload$startStatus(ChunkStatus status);
}
