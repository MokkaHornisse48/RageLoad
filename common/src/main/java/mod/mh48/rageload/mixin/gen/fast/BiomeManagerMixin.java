package mod.mh48.rageload.mixin.gen.fast;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import mod.mh48.rageload.client.worldgen.ClientChunkGenerator;
import mod.mh48.rageload.client.worldgen.RageConfig;
import mod.mh48.rageload.client.worldgen.fakedata.ClientGenServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeManager.class)
public class BiomeManagerMixin {

    @Unique
    public ThreadLocal<Long2ObjectOpenHashMap<Holder<Biome>>> rageLoad$cache = ThreadLocal.withInitial(Long2ObjectOpenHashMap::new);

    @Redirect(method = "getBiome",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeManager$NoiseBiomeSource;getNoiseBiome(III)Lnet/minecraft/core/Holder;"))
    public Holder<Biome> getNoiseBiomeRedirect(BiomeManager.NoiseBiomeSource instance, int x, int y, int z) {

        if(RageConfig.biomes2d()) {
            ServerLevel level;
            if (instance instanceof ServerLevel plevel && plevel.getServer() instanceof ClientGenServer) {
                level = plevel;
            }
            else if (instance instanceof ClientChunkGenerator.WorldGenRegionNoBlending region) {
                level = region.getLevel();
            }
            else{
                level = null;
            }
            if (level != null) {
                Long2ObjectOpenHashMap<Holder<Biome>> c = rageLoad$cache.get();
                if(c.size()>1000) {
                    c.clear();
                }
                long pos = BlockPos.asLong(x, y, z);
                return c.computeIfAbsent(pos,(l) -> level.getUncachedNoiseBiome(x,y,z));
            }
        }
        return instance.getNoiseBiome(x, y, z);
    }

}
