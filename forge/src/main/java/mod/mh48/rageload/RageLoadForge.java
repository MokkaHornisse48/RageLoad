package mod.mh48.rageload;

import mod.mh48.rageload.client.ClientCommandRegistration;
import mod.mh48.rageload.compat.CompatHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RageLoad.MOD_ID)
public class RageLoadForge {
    
    public RageLoadForge() {

        CompatHandler.preInit();
        RageLoad.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetupRedirect::init);

    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new ClientCommandRegistration<>(event.getDispatcher());
    }
}