package mod.mh48.rageload.mixin.gen.fast;

import mod.mh48.rageload.client.worldgen.faster.FakeNoiseChunk;
import mod.mh48.rageload.duck.gen.fast.BiomeRule2d;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource")
public class BiomeConditionSourceMixin {

    @Mutable
    @Shadow
    @Final
    Predicate<ResourceKey<Biome>> biomeNameTest;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(List<ResourceKey<Biome>> biomes,CallbackInfo ci) {
        if(biomes.isEmpty()){
            biomeNameTest = (biomeTest) -> false;
        }
        if(biomes.size()==1){
            ResourceKey<Biome> biome = biomes.get(0);
            biomeNameTest = (biomeTest) -> biome==biomeTest;
        }
        if(biomes.size()==2){
            ResourceKey<Biome> biome0 = biomes.get(0);
            ResourceKey<Biome> biome1 = biomes.get(1);
            biomeNameTest = (biomeTest) -> biome0==biomeTest || biome1==biomeTest;
        }
    }

    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;",at = @At("HEAD"),cancellable = true)
    private void onApply(SurfaceRules.Context p_context, CallbackInfoReturnable<SurfaceRules.Condition> cir){
        SurfaceRulesAccess ctx = (SurfaceRulesAccess)(Object) p_context;
        if(ctx.getNoiseChunk() instanceof FakeNoiseChunk){
            class BiomeCondition implements SurfaceRules.Condition, BiomeRule2d {

                public boolean state = false;

                @Override
                public void update(ResourceKey<Biome> biome) {
                    state = biomeNameTest.test(biome);
                }

                @Override
                public boolean test() {
                    return state;
                }
            }
            BiomeCondition bc = new BiomeCondition();
            ctx.rageLoad$getBiomeRules().add(bc);
            cir.setReturnValue(bc);
            cir.cancel();
        }
    }
}
