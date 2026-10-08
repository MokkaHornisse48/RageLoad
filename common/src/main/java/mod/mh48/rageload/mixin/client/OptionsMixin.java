package mod.mh48.rageload.mixin.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {

    @Shadow public abstract OptionInstance<Integer> renderDistance();

    @Inject(method = "getEffectiveRenderDistance",at = @At("RETURN"),cancellable = true)
    public void onGetEffectiveRenderDistance(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.renderDistance().get());
        cir.cancel();

    }


}
