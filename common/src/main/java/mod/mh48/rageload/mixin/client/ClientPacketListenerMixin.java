package mod.mh48.rageload.mixin.client;

import com.mojang.authlib.GameProfile;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.client.NetworkingClient;
import mod.mh48.rageload.client.worldgen.ClientChunkGenerator;
import mod.mh48.rageload.client.worldgen.fakedata.ClientGenerationInitializer;
import mod.mh48.rageload.duck.client.ClientPacketListenerDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin implements ClientPacketListenerDuck {

    @Shadow
    @Final
    private Connection connection;

    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private ClientChunkGenerator generator;

    @Inject(method = "handleLogin",at = @At("RETURN"))
    public void onInit(ClientboundLoginPacket packet, CallbackInfo ci){
        if(!minecraft.isLocalServer()){
            new Thread(() -> {
                try {
                    NetworkingClient.initWorldDataData(minecraft.options.renderDistance().get());

                    CompletableFuture<CompoundTag> f = NetworkingClient.requestGenData();
                    ClientGenerationInitializer initGen = new ClientGenerationInitializer(f.join());
                    generator = initGen.initGenerator();
                } catch (Exception e) {
                    RageLoad.LOGGER.warn("Error initializing ClientChunkGenerator: ",e);
                }
            },"Create client WorldGenServer").start();
        }
    }

    @Inject(method = "handleForgetLevelChunk",at = @At("HEAD"),cancellable = true)
    public void onHandleForgetLevelChunk(ClientboundForgetLevelChunkPacket pPacket, CallbackInfo ci){
        ci.cancel();
    }

    @Inject(method = "close",at = @At("RETURN"))
    public void onClose(CallbackInfo ci) {
        if(generator!=null){
            new Thread(() -> {
                generator.unload();
            },"save gen server").start();
        }
        NetworkingClient.clear();
    }

    @Unique
    @Override
    public ClientChunkGenerator getGenerator() {
        return generator;
    }
}
