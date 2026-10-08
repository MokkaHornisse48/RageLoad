package mod.mh48.rageload.mixin.net;

import mod.mh48.rageload.net.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleCustomPayload",at = @At("HEAD"),cancellable = true)
    public void onHandleCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci){
        ClientNetworkManager.Receiver r = ClientNetworkManager.receivers.get(packet.getIdentifier());
        if(r != null){
            ci.cancel();
            r.receive(packet.getData(),this.minecraft);
            //packet.getData().release();
        }
    }
}
