package mod.mh48.rageload.mixin.debug;

import mod.mh48.rageload.duck.client.ClientPacketListenerDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Shadow
    protected abstract Level getLevel();

    @Shadow
    @Nullable
    protected abstract ServerLevel getServerLevel();

    @Inject(method = "getServerLevel",at = @At("RETURN"),cancellable = true)
    public void onLevel(CallbackInfoReturnable<ServerLevel> cir) {
        if(cir.getReturnValue() == null)cir.setReturnValue(((ClientPacketListenerDuck) Minecraft.getInstance().getConnection()).getGenerator().server.getLevel(Minecraft.getInstance().level.dimension()));
    }

    @Inject(method = "getGameInformation",at = @At("RETURN"))
    public void onGetGameInformation(CallbackInfoReturnable<List<String>> cir){
        List<String> info = cir.getReturnValue();

        BlockPos pos = Minecraft.getInstance().player.blockPosition();

        ServerLevel sl = getServerLevel();
        if(sl == null) return;
        long seed = sl.getSeed();

        int absX = pos.getX() - 2;
        int absY = pos.getY() - 2;
        int absZ = pos.getZ() - 2;
        int parentX = absX >> 2;
        int parentY = absY >> 2;
        int parentZ = absZ >> 2;
        double fractX = (absX & 3) / 4.0;
        double fractY = (absY & 3) / 4.0;
        double fractZ = (absZ & 3) / 4.0;
        int minI = 0;
        double minFiddledDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; i++) {
            boolean xEven = (i & 4) == 0;
            boolean yEven = (i & 2) == 0;
            boolean zEven = (i & 1) == 0;
            int cornerX = xEven ? parentX : parentX + 1;
            int cornerY = yEven ? parentY : parentY + 1;
            int cornerZ = zEven ? parentZ : parentZ + 1;
            double distanceX = xEven ? fractX : fractX - 1.0;
            double distanceY = yEven ? fractY : fractY - 1.0;
            double distanceZ = zEven ? fractZ : fractZ - 1.0;
            double next = rageLoad$getFiddledDistance(BiomeManager.obfuscateSeed(seed), cornerX, cornerY, cornerZ, distanceX, distanceY, distanceZ);
            if (minFiddledDistance > next) {
                minI = i;
                minFiddledDistance = next;
            }
        }

        int biomeX = (minI & 4) == 0 ? parentX : parentX + 1;
        int biomeY = (minI & 2) == 0 ? parentY : parentY + 1;
        int biomeZ = (minI & 1) == 0 ? parentZ : parentZ + 1;

        info.add("Biome X: %d Y: %d Z: %d Biome Uncached: ".formatted(biomeX, biomeY, biomeZ) + sl.getUncachedNoiseBiome(biomeX, biomeY, biomeZ).unwrapKey().get().location());
    }

    @Unique
    private static double rageLoad$getFiddledDistance(
            final long seed, final int xRandom, final int yRandom, final int zRandom, final double distanceX, final double distanceY, final double distanceZ
    ) {
        long rval = LinearCongruentialGenerator.next(seed, xRandom);
        rval = LinearCongruentialGenerator.next(rval, yRandom);
        rval = LinearCongruentialGenerator.next(rval, zRandom);
        rval = LinearCongruentialGenerator.next(rval, xRandom);
        rval = LinearCongruentialGenerator.next(rval, yRandom);
        rval = LinearCongruentialGenerator.next(rval, zRandom);
        double fiddleX = rageLoad$getFiddle(rval);
        rval = LinearCongruentialGenerator.next(rval, seed);
        double fiddleY = rageLoad$getFiddle(rval);
        rval = LinearCongruentialGenerator.next(rval, seed);
        double fiddleZ = rageLoad$getFiddle(rval);
        return Mth.square(distanceZ + fiddleZ) + Mth.square(distanceY + fiddleY) + Mth.square(distanceX + fiddleX);
    }

    @Unique
    private static double rageLoad$getFiddle(final long rval) {
        double uniform = Math.floorMod(rval >> 24, 1024) / 1024.0;
        return (uniform - 0.5) * 0.9;
    }
}
