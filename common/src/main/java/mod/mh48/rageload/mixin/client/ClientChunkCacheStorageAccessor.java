package mod.mh48.rageload.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkCache.Storage.class)
public interface ClientChunkCacheStorageAccessor {
    @Accessor("chunks")
    AtomicReferenceArray<LevelChunk> getChunks();
    @Accessor("viewCenterX")
    int getViewCenterX();
    @Accessor("viewCenterZ")
    int getViewCenterZ();
    @Accessor("viewRange")
    int getViewRange();
    @Accessor("chunkRadius")
    int getChunkRadius();
    @Invoker("replace")
    void doReplace(int pChunkIndex, LevelChunk pChunk);
    @Invoker("inRange")
    boolean isInRange(int pChunkX, int pChunkZ);
    @Invoker("getIndex")
    int doGetIndex(int pChunkX, int pChunkZ);


}
