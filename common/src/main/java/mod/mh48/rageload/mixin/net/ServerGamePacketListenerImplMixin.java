package mod.mh48.rageload.mixin.net;

import mod.mh48.rageload.net.NetworkManager;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleCustomPayload",at = @At("HEAD"), cancellable = true)
    public void onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci){
        NetworkManager.Receiver r = NetworkManager.receivers.get(packet.getIdentifier());
        if(r != null){
            ci.cancel();
            r.receive(packet.getData(),this.player);
            //packet.getData().release();
        }
    }
}
