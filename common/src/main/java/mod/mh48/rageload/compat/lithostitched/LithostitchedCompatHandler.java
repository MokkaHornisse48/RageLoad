package mod.mh48.rageload.compat.lithostitched;

import dev.worldgen.lithostitched.registry.LithostitchedRegistryKeys;
import dev.worldgen.lithostitched.worldgen.densityfunction.MergedDensityFunction;
import dev.worldgen.lithostitched.worldgen.modifier.Modifier;
import dev.worldgen.lithostitched.worldgen.modifier.WrapDensityFunctionModifier;
import dev.worldgen.lithostitched.worldgen.modifier.WrapNoiseRouterModifier;
import dev.worldgen.lithostitched.worldgen.surface.SurfaceRuleManager;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.WorldGenData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class LithostitchedCompatHandler {


    public static void init() {
        WorldGenData.rc(LithostitchedRegistryKeys.WORLDGEN_MODIFIER, Modifier.CODEC);
    }

    public static void onServerAboutToStart(MinecraftServer server) {
        //Modifier.applyModifiers(server); //alrerady aplied on server side
        applyModifiers(server);
        SurfaceRuleManager.applySurfaceRules(server);
    }


    public static void applyModifiers(MinecraftServer server) {
        try {


            Method applyPhaseModifiers = Modifier.class.getDeclaredMethod(
                    "applyPhaseModifiers",
                    RegistryAccess.class,
                    List.class
            );
            applyPhaseModifiers.setAccessible(true);

            RegistryAccess registries = server.registryAccess();
            HolderLookup.RegistryLookup<Modifier> modifiers = registries.lookupOrThrow(LithostitchedRegistryKeys.WORLDGEN_MODIFIER);

            for (Modifier.ModifierPhase phase : Modifier.ModifierPhase.values()) {
                if (phase != Modifier.ModifierPhase.NONE) {
                    List<Holder.Reference<Modifier>> phaseModifiers = modifiers.listElements().filter((m) -> m.value().getPhase() == phase &&
                            (m.value() instanceof WrapDensityFunctionModifier || m.value() instanceof WrapNoiseRouterModifier)
                            ).toList();


                    applyPhaseModifiers.invoke(null, registries, phaseModifiers);
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
