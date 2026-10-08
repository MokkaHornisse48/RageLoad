package mod.mh48.rageload.mixin.gen.fast;

import com.google.common.collect.ImmutableMap;
import mod.mh48.rageload.client.worldgen.faster.FakeNoiseChunk;
import mod.mh48.rageload.duck.gen.fast.BiomeRule2d;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(NamespacedSurfaceRuleSource.class)
public class NamespacedSurfaceRuleSourceMixin {

    @Shadow
    @Final
    private Map<String, SurfaceRules.RuleSource> sources;

    @Shadow
    @Final
    private SurfaceRules.RuleSource base;

    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$SurfaceRule;",at = @At("HEAD"),cancellable = true,remap = false)
    public void onApply(SurfaceRules.Context context, CallbackInfoReturnable<SurfaceRules.SurfaceRule> cir) {
        SurfaceRulesAccess ctx = (SurfaceRulesAccess)(Object) context;
        if(ctx.getNoiseChunk() instanceof FakeNoiseChunk) {
            ImmutableMap.Builder<String, SurfaceRules.SurfaceRule> rules = new ImmutableMap.Builder<>();
            this.sources.forEach((key, value) -> rules.put(key, value.apply(context)));
            NamespacedRule2d nr = new NamespacedRule2d(context, this.base.apply(context), rules.build());
            ctx.rageLoad$getBiomeRules().add(nr);
            cir.setReturnValue(nr);
            cir.cancel();
        }
    }

    static class NamespacedRule2d implements SurfaceRules.SurfaceRule, BiomeRule2d {

        public final SurfaceRules.Context context;
        public final SurfaceRules.SurfaceRule baseRule;
        public final Map<String, SurfaceRules.SurfaceRule> rules;

        SurfaceRules.SurfaceRule rule = null;

        public NamespacedRule2d(SurfaceRules.Context context, SurfaceRules.SurfaceRule baseRule, Map<String, SurfaceRules.SurfaceRule> rules) {
            this.context = context;
            this.baseRule = baseRule;
            this.rules = rules;
        }

        @Override
        public void update(ResourceKey<Biome> biome) {
            rule = this.rules.getOrDefault(biome.location().getNamespace(), null);
        }

        @Nullable
        public BlockState tryApply(int x, int y, int z) {
            BlockState state = null;
            if (rule != null) {
                state = rule.tryApply(x, y, z);
            }

            if (state == null) {
                state = this.baseRule.tryApply(x, y, z);
            }

            return state;
        }


    }
}
