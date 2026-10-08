package mod.mh48.rageload.mixin.gen.fakedata;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import mod.mh48.rageload.client.worldgen.fakedata.ClientGenServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {

    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;
    @Shadow
    @Final
    public ChunkMap chunkMap;
    @Shadow
    @Final
    private ServerLevel level;
    @Unique
    public EmptyLevelChunk emptyChunk;

    @Unique
    public ChunkHolder emptyChunkHolder;

    @Inject(method = "<init>",at = @At("RETURN"))
    public void onInit(ServerLevel serverLevel, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor executor, ChunkGenerator chunkGenerator, int i, int j, boolean bl, ChunkProgressListener chunkProgressListener, ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier supplier, CallbackInfo ci){
        if(notGen())return;
        this.emptyChunk = new EmptyLevelChunk(serverLevel, new ChunkPos(0, 0), serverLevel.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
        this.emptyChunkHolder = new ChunkHolder(new ChunkPos(0, 0),0,serverLevel,this.lightEngine,( chunkPos,  intSupplier,  i2321,  intConsumer)->{},this.chunkMap);
    }

    @Unique
    public boolean notGen(){
        return !(level.getServer() instanceof ClientGenServer);
    }

    @Inject(method = "getVisibleChunkIfPresent",at = @At("HEAD"),cancellable = true)
    private void onGetVisibleChunkIfPresent(long l, CallbackInfoReturnable<ChunkHolder> cir) {
        if(notGen())return;
        cir.setReturnValue(emptyChunkHolder);
    }


    @Inject(method = "getChunk",at = @At("HEAD"),cancellable = true)
    public void onGetChunk(int i, int j, ChunkStatus chunkStatus, boolean bl, CallbackInfoReturnable<ChunkAccess> cir) {
        if(notGen())return;
        cir.setReturnValue(emptyChunk);
    }

    @Inject(method = "getChunkNow",at = @At("HEAD"),cancellable = true)
    public void onGetChunkNow(int i, int j, CallbackInfoReturnable<LevelChunk> cir) {
        if(notGen())return;
        cir.setReturnValue(emptyChunk);
    }

    @Inject(method = "getChunkFuture",at = @At("HEAD"),cancellable = true)
    public void onGetChunkFuture(int i, int j, ChunkStatus chunkStatus, boolean bl, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        if(notGen())return;
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = new CompletableFuture<>();
        future.complete(Either.left(emptyChunk));
        cir.setReturnValue(future);
    }

    @Inject(method = "hasChunk",at = @At("HEAD"),cancellable = true)
    public void onHasChunk(int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if(notGen())return;
        cir.setReturnValue(true);
    }

    @Inject(method = "isPositionTicking",at = @At("HEAD"),cancellable = true)
    public void onIsPositionTicking(long l, CallbackInfoReturnable<Boolean> cir) {
        if(notGen())return;
        cir.setReturnValue(true);
    }

    @Inject(method = "addRegionTicket",at = @At("HEAD"),cancellable = true)
    public <T> void onAddRegionTicket(TicketType<T> ticketType, ChunkPos chunkPos, int i, T object, CallbackInfo ci) {
        if(notGen())return;
        ci.cancel();
    }

    @Inject(method = "addEntity",at = @At("HEAD"),cancellable = true)
    public void onAddEntity(Entity entity, CallbackInfo ci) {
        if(notGen())return;
        ci.cancel();
    }

}
