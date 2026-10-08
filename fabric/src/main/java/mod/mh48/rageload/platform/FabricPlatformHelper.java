package mod.mh48.rageload.platform;

import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        boolean isLoaded = FabricLoader.getInstance().isModLoaded(modId);
        return isLoaded;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
