package mod.mh48.rageload.mixin.gen.fast;

import mod.mh48.rageload.client.worldgen.faster.FakeNoiseChunk;
import mod.mh48.rageload.duck.gen.fast.BiomeRule2d;
import mod.mh48.rageload.duck.gen.fast.SurfaceRulesContextDuck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.Function;

@Mixin(SurfaceRules.Context.class)
public class SurfaceRulesContextMixin implements SurfaceRulesContextDuck {

    @Shadow
    @Final
    private NoiseChunk noiseChunk;
    @Shadow
    @Final
    private Function<BlockPos, Holder<Biome>> biomeGetter;
    @Shadow
    @Final
    private ChunkAccess chunk;
    @Unique
    public ArrayList<BiomeRule2d> rageLoad$biomeRules = new ArrayList<>();
    @Unique
    public BlockPos.MutableBlockPos bbp = new BlockPos.MutableBlockPos();


    @Unique
    public ArrayList<BiomeRule2d> rageLoad$getBiomeRules() {
        return rageLoad$biomeRules;
    }

    @Inject(method = "updateXZ",at = @At("HEAD"))
    public void onUpdateXZ(int blockX, int blockZ, CallbackInfo ci){
        if(noiseChunk instanceof FakeNoiseChunk){
            int surfaceY = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                    .getHighestTaken(blockX & 15, blockZ & 15);
            bbp.set(blockX,surfaceY,blockZ);
            ResourceKey<Biome> biome = ((Holder.Reference<Biome>)biomeGetter.apply(bbp)).key();
            rageLoad$biomeRules.forEach(rageLoad$biomeRule -> {
                rageLoad$biomeRule.update(biome);
            });
        }
    }
}
