package mod.mh48.rageload.net;

import com.mojang.logging.LogUtils;
import mod.mh48.rageload.RageLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.HashMap;

public class ClientNetworkManager {

    public static HashMap<ResourceLocation, Receiver> receivers = new HashMap<>();

    public static void registerReceiver(ResourceLocation location, Receiver receiver){
        receivers.put(location, receiver);
    }

    public static void sendToServer(ResourceLocation location, FriendlyByteBuf data) {
        if(Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(location, data));
        }else{
            RageLoad.LOGGER.error("Couldn't send Custom Payload to Server "+location);
        }

    }

    @FunctionalInterface
    public interface Receiver {
        void receive(FriendlyByteBuf buf, Minecraft mc);
    }
}
