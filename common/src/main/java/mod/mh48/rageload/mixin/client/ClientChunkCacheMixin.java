package mod.mh48.rageload.mixin.client;

import mod.mh48.rageload.client.ClientWorldLoader;
import mod.mh48.rageload.client.NetworkingClient;
import mod.mh48.rageload.duck.client.ClientChunkCacheDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin implements AutoCloseable, ClientChunkCacheDuck {

    @Shadow private volatile ClientChunkCache.Storage storage;

    @Shadow @Final private ClientLevel level;

    @Unique
    public ClientWorldLoader clientWorldLoader;

    @Unique
    public int oldViewDistance = 0;

    @Inject(method = "<init>",at = @At("RETURN"))
    public void onInit(ClientLevel pLevel, int pViewDistance, CallbackInfo ci){

        Minecraft mc = Minecraft.getInstance();
        if(mc.isLocalServer()){

        }else{
            clientWorldLoader = new ClientWorldLoader(pLevel,()->this.storage);
        }

        oldViewDistance = Minecraft.getInstance().options.renderDistance().get();

    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void onTick(BooleanSupplier pHasTimeLeft, boolean pTickChunks, CallbackInfo ci) {
        if(oldViewDistance != Minecraft.getInstance().options.renderDistance().get()) {
            oldViewDistance = Minecraft.getInstance().options.renderDistance().get();

            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener handler = mc.getConnection(); // must be non-null
            if (handler != null) {
                handler.handleSetChunkCacheRadius(new ClientboundSetChunkCacheRadiusPacket(Math.max(2, oldViewDistance) + 3));
                NetworkingClient.initWorldDataData(oldViewDistance);
            }
        }
        if(clientWorldLoader!=null) {
            clientWorldLoader.tick();
        }
    }


    @Inject(method = "updateViewCenter",at = @At("HEAD"))
    public void onUpdateViewCenter(int pX, int pZ, CallbackInfo ci){
        if(clientWorldLoader!=null) {
        }
    }

    @Override
    public void close(){
        if(clientWorldLoader!=null) {
            clientWorldLoader.close();
        }
    }

    @Override
    public ClientWorldLoader getWorldLoader(){
        return clientWorldLoader;
    }
}
