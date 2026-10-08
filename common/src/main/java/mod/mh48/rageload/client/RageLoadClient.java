package mod.mh48.rageload.client;

import com.mojang.brigadier.CommandDispatcher;
import mod.mh48.rageload.duck.client.ClientPacketListenerDuck;
import mod.mh48.rageload.duck.debug.ProtoChunkStats;
import mod.mh48.rageload.mixin.debug.MultiNoiseBiomeSourceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

public class RageLoadClient {
    public static void init(){
        NetworkingClient.init();

        /*
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, ctx) -> {
            dispatcher.register(ClientCommandRegistrationEvent.literal("CLdebugDF") // Der Name des Commands
                    .executes(context -> {
                        ServerLevel serverLevel = ((ClientPacketListenerDuck) Minecraft.getInstance().getConnection()).getGenerator().server.getLevel(Minecraft.getInstance().level.dimension());
                        ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
                        RandomState randomState = serverChunkCache.randomState();
                        BlockPos blockPos = BlockPos.containing(context.getSource().arch$getPosition());
                        ArrayList<String> list = new ArrayList<>();
                        NoiseRouter noiseRouter = randomState.router();
                        DensityFunction.FunctionContext singlePointContext = new DensityFunction.SinglePointContext(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        ((SinglePointContextDuck)singlePointContext).setDebugInfo(list);
                        noiseRouter.initialDensityWithoutJaggedness().compute(singlePointContext);
                        for(String el:list){
                            context.getSource().arch$sendSuccess(() -> Component.literal(el), true);
                        }
                        return 1;
                    })
            );
        });*/

    }

}
