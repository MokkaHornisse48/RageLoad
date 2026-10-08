package mod.mh48.rageload.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundLoginPacket.class)
public class ClientboundLoginPacketMixin {

    @Mutable
    @Shadow
    @Final
    private int chunkRadius;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(FriendlyByteBuf pBuffer, CallbackInfo ci){
        this.chunkRadius = Minecraft.getInstance().options.renderDistance().get();
    }
}
