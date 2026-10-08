package mod.mh48.rageload.mixin.gen.fakedata;

import mod.mh48.rageload.client.worldgen.fakedata.FakeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;


@Mixin(Holder.Reference.class)
public class HolderReferenceMixin<T> {

    @Shadow
    private T value;

    @Shadow
    @Final
    private HolderOwner<T> owner;

    @Shadow
    private ResourceKey<T> key;

    @Inject(method = "value",at = @At("HEAD"))
    public void onGetValue(CallbackInfoReturnable<T> cir) {
        if (this.value == null && this.owner instanceof FakeRegistry<T> fakeRegistry) {
            this.value = fakeRegistry.get(this.key);
        }
    }

    @Inject(method = "isBound",at = @At("HEAD"))
    public void onIsBound(CallbackInfoReturnable<T> cir) {
        if (this.value == null && this.owner instanceof FakeRegistry<T> fakeRegistry) {
            this.value = fakeRegistry.get(this.key);
        }
    }

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z",at = @At("RETURN"),cancellable = true)
    public void onIsTag(TagKey<T> tagKey, CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValueZ()&&this.owner instanceof FakeRegistry<T> fakeRegistry){
            Optional<HolderSet.Named<T>> tag = fakeRegistry.getTag(tagKey);
            if(tag.isPresent()) {
                if(((HolderSetNamedAccessor)tag.get()).getContents().contains(this)){
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
