package mod.mh48.rageload.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.worldgen.lithostitched.registry.LithostitchedRegistryKeys;
import dev.worldgen.lithostitched.worldgen.modifier.WrapNoiseRouterModifier;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.WorldGenData;
import mod.mh48.rageload.duck.client.ClientPacketListenerDuck;
import mod.mh48.rageload.duck.debug.ProtoChunkStats;
import mod.mh48.rageload.mixin.debug.MultiNoiseBiomeSourceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.potionstudios.biomeswevegone.BiomesWeveGone;

import java.util.List;

public class ClientCommandRegistration<T extends SharedSuggestionProvider> {

    public LiteralArgumentBuilder<T> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public <E> RequiredArgumentBuilder<T, E> argument(String name, ArgumentType<E> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public void sendMsg(String msg){
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(msg));
    }

    public ClientCommandRegistration(CommandDispatcher<T> dispatcher){
        dispatcher.register(this.literal("crageload")
                .then(this.literal("biome")
                        .executes(context -> {
                            ServerLevel serverLevel = ((ClientPacketListenerDuck) Minecraft.getInstance().getConnection()).getGenerator().server.getLevel(Minecraft.getInstance().level.dimension());
                            ServerChunkCache serverChunkCache = serverLevel.getChunkSource();

                            Climate.ParameterList<Holder<Biome>> params = ((MultiNoiseBiomeSourceAccessor) serverChunkCache.getGenerator().getBiomeSource()).invokeParameters();

                            this.sendMsg("Debug Biome Params: " + params.values());
                            return 1;
                        })
                )
                .then(this.literal("genstats")
                        .executes(context -> {
                            ProtoChunkStats.logAverages(this::sendMsg);
                            return 1;
                        })
                )
        );
    }
}
