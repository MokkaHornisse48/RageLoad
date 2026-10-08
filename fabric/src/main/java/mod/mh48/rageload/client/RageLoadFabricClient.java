package mod.mh48.rageload.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class RageLoadFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RageLoadClient.init();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            new ClientCommandRegistration<>(dispatcher);
        });
    }
}
