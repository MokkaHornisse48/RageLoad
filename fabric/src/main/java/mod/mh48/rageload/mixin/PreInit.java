package mod.mh48.rageload.mixin;

import mod.mh48.rageload.compat.CompatHandler;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class PreInit {

    @Inject(method = "bootStrap",at = @At("RETURN"))
    private static void preInit(CallbackInfo ci) {
        CompatHandler.preInit();
    }
}
