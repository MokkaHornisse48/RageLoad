package mod.mh48.rageload.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;

public class NetworkManager {

    public static HashMap<ResourceLocation, Receiver> receivers = new HashMap<>();

    public static void registerReceiver(ResourceLocation location, Receiver receiver){
        receivers.put(location, receiver);
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation location, FriendlyByteBuf data) {
        player.connection.send(new ClientboundCustomPayloadPacket(location, data));
    }

    @FunctionalInterface
    public interface Receiver {
        void receive(FriendlyByteBuf buf, ServerPlayer player);
    }
}
