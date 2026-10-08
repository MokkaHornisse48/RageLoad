package mod.mh48.rageload.mixin;

import mod.mh48.rageload.client.worldgen.fakedata.ClientGenServer;
import mod.mh48.rageload.client.worldgen.fakedata.FakeRegistry;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeModificationImpl.class)
public class BiomeModificationImplMixin {
    @Inject(method = "finalizeWorldGen", at = @At("HEAD"), cancellable = true)
    private void rageload$skipOnFakeServer(RegistryAccess impl, CallbackInfo ci) {
        if (impl.registryOrThrow(Registries.BIOME) instanceof FakeRegistry<Biome>) ci.cancel();
    }
}
