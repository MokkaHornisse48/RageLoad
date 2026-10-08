package mod.mh48.rageload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;

public class RageLoadFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        RageLoad.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) -> {
            RageLoad.registerCommands(dispatcher);
        });
    }
}
