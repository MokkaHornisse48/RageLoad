package mod.mh48.rageload.mixin.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import mod.mh48.rageload.duck.server.RegistryOpsDuck;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(RegistryFileCodec.class)

public abstract class RegistryFileCodecMixin<E> implements Codec<Holder<E>> {

    @Shadow
    @Final
    private Codec<E> elementCodec;

    @Shadow
    @Final
    private ResourceKey<? extends Registry<E>> registryKey;

    @Inject(method = "encode(Lnet/minecraft/core/Holder;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;",at = @At("HEAD"),cancellable = true)
    public <T> void onEncode(Holder<E> pInput, DynamicOps<T> pOps, T pPrefix, CallbackInfoReturnable<DataResult<T>> cir) {
        if (pOps instanceof RegistryOpsDuck rd && rd.isForGenData()) {
            if (pInput instanceof Holder.Reference<E> reference) {
                //Optiload.LOGGER.info(this.registryKey.toString());
                DataResult<T> a = pInput.unwrap().map((p_206714_) -> {
                    return ResourceLocation.CODEC.encode(p_206714_.location(), pOps, pPrefix);
                }, (p_206710_) -> {
                    return this.elementCodec.encode(p_206710_, pOps, pPrefix);
                });
                cir.setReturnValue(a);
                cir.cancel();
            }
        }
    }
}
