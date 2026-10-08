package mod.mh48.rageload.mixin.gen.fast;

import mod.mh48.rageload.duck.gen.fast.SurfaceRulesContextDuck;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesAccess extends SurfaceRulesContextDuck {
    @Accessor("noiseChunk")
    NoiseChunk getNoiseChunk();
}
