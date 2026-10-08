package mod.mh48.rageload;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import mod.mh48.rageload.compat.CompatHandler;
import mod.mh48.rageload.mixin.debug.MultiNoiseBiomeSourceAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class RageLoad {

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "rageload";
    public static final String MOD_NAME = "RageLoad";


    public static void init() {
        Networking.init();
        CompatHandler.init();

        /*
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection)-> {
            dispatcher.register(Commands.literal("debugDF") // Der Name des Commands
                    .executes(context -> {
                        ServerLevel serverLevel = context.getSource().getLevel();
                        ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
                        RandomState randomState = serverChunkCache.randomState();
                        BlockPos blockPos = BlockPos.containing(context.getSource().getPosition());
                        ArrayList<String> list = new ArrayList<>();
                        NoiseRouter noiseRouter = randomState.router();
                        DensityFunction.FunctionContext singlePointContext = new DensityFunction.SinglePointContext(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        ((SinglePointContextDuck)singlePointContext).setDebugInfo(list);
                        noiseRouter.initialDensityWithoutJaggedness().compute(singlePointContext);
                        for(String el:list){
                            context.getSource().sendSuccess(() -> Component.literal(el), true);
                        }
                        return 1;
                    })
            );
        });*/


    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("rageload")
                    // --- SUBCOMMAND: worldgendata ---
                    .then(Commands.literal("save-data")
                            .executes(context -> {
                                try {
                                    CompoundTag data = WorldGenData.generateWorldGenData(context.getSource().getServer());
                                    File file = new File("WorldGenData.nbt");
                                    NbtIo.writeCompressed(data, file);
                                    context.getSource().sendSuccess(() -> Component.literal("NBT Daten komprimiert gespeichert in: " + file.getName()), true);
                                } catch (Exception e) {
                                    context.getSource().sendFailure(Component.literal("Fehler beim Speichern der NBT-Datei!"));
                                    e.printStackTrace();
                                }
                                return 1;
                            })
                    )
                    // --- SUBCOMMAND: noiserouter ---
                    .then(Commands.literal("noiserouter")
                            .executes(context -> {
                                ServerLevel serverLevel = context.getSource().getLevel();
                                ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
                                ChunkGenerator chunkGenerator = serverChunkCache.getGenerator();
                                RandomState randomState = serverChunkCache.randomState();
                                List<String> list = new ArrayList<>();
                                chunkGenerator.addDebugScreenInfo(list, randomState, BlockPos.containing(context.getSource().getPosition()));

                                for (String el : list) {
                                    context.getSource().sendSuccess(() -> Component.literal(el), true);
                                }
                                return 1;
                            })
                    )
                    // --- SUBCOMMAND: biome ---
                    .then(Commands.literal("biome")
                            .executes(context -> {
                                ServerLevel serverLevel = context.getSource().getLevel();
                                ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
                                Climate.ParameterList<Holder<Biome>> params = ((MultiNoiseBiomeSourceAccessor) serverChunkCache.getGenerator().getBiomeSource()).invokeParameters();

                                context.getSource().sendSuccess(() -> Component.literal("Debug Biome Params: " + params.values()), true);
                                return 1;
                            })
                    )
            );
    }

    public static final Unsafe unsafe;

    static{
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ResourceLocation of(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
