package mod.mh48.rageload.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    @Shadow
    public abstract void queueLightUpdate(Runnable pTask);

    protected ClientLevelMixin(WritableLevelData pLevelData, ResourceKey<Level> pDimension, RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration, Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed, int pMaxChainedNeighborUpdates) {
        super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug, pBiomeZoomSeed, pMaxChainedNeighborUpdates);
    }

    @Inject(method = "unload",at = @At("RETURN"))
    public void onUnload(LevelChunk pChunk, CallbackInfo ci) {
        this.queueLightUpdate(() -> {
            LevelLightEngine levellightengine = this.getLightEngine();
            levellightengine.setLightEnabled(pChunk.getPos(), false);
            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); ++i) {
                SectionPos sectionpos = SectionPos.of(pChunk.getPos(), i);
                levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, (DataLayer) null);
                levellightengine.queueSectionData(LightLayer.SKY, sectionpos, (DataLayer) null);
            }
            for (int j = this.getMinSection(); j < this.getMaxSection(); ++j) {
                levellightengine.updateSectionStatus(SectionPos.of(pChunk.getPos(), j), true);
            }
        });
    }
}
