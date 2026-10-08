package mod.mh48.rageload.compat.biomeswevegone;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Noises;
import net.potionstudios.biomeswevegone.world.level.levelgen.customterrain.BasaltBarreraExtension;
import net.potionstudios.biomeswevegone.world.level.levelgen.customterrain.CragGardenExtension;

public class BiomeswevegoneCompatHandler {
    public static void onSurface(ServerLevel serverLevel, ChunkGenerator chunkGenerator, ChunkAccess chunkAccess, WorldGenRegion worldGenRegion){
        CragGardenExtension.runCragGardenExtension(worldGenRegion::getBiome, chunkAccess, serverLevel.getSeed(), worldGenRegion.registryAccess().registryOrThrow(Registries.NOISE).getOrThrow(Noises.SURFACE), worldGenRegion.registryAccess().registryOrThrow(Registries.NOISE).getOrThrow(Noises.SURFACE_SECONDARY));
        BasaltBarreraExtension.runBasaltBarreraExtension(chunkAccess, worldGenRegion, chunkGenerator);

    }
}
