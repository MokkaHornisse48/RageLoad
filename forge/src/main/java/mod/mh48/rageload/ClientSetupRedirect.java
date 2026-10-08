package mod.mh48.rageload;

import mod.mh48.rageload.client.ClientCommandRegistration;
import mod.mh48.rageload.client.RageLoadClient;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


public class ClientSetupRedirect {

    public static void init(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(ClientSetupRedirect.class);
        RageLoadClient.init();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        new ClientCommandRegistration<>(event.getDispatcher());
    }
}
