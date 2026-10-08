package mod.mh48.rageload.mixin.gen;

import mod.mh48.rageload.client.worldgen.SaveProtoChunk;
import mod.mh48.rageload.duck.gen.LevelChunkDuck;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements LevelChunkDuck {
    @Unique
    public boolean wasGeneratedByClient = false;

    @Override
    public boolean wasGeneratedByClient() {
        return wasGeneratedByClient;
    }

    @Override
    public void setGeneratedByClient(boolean gen) {
        wasGeneratedByClient = gen;
    }

    @Unique
    public SaveProtoChunk save;

    @Override
    public void setSave(SaveProtoChunk save) {
        this.save = save;
    }

    @Override
    public SaveProtoChunk getSave() {
        return this.save;
    }
}
