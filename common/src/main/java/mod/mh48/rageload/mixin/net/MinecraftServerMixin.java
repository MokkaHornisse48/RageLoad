package mod.mh48.rageload.mixin.net;

import mod.mh48.rageload.Networking;
import mod.mh48.rageload.client.worldgen.fakedata.ClientGenServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "tickServer",at = @At("RETURN"))
    public void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci){
        if((Object)this instanceof ClientGenServer) return;

        Networking.tick((MinecraftServer)(Object)this);
    }

    @Inject(method = "runServer",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z",shift = At.Shift.AFTER))
    private void afterInitServer(CallbackInfo info) {
        if((Object)this instanceof ClientGenServer) return;
        Networking.initGenData((MinecraftServer)(Object)this);
    }
}
