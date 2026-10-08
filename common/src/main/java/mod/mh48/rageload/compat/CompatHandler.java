package mod.mh48.rageload.compat;

import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.compat.lithostitched.LithostitchedCompatHandler;
import mod.mh48.rageload.compat.tectonic.TectonicCompatHandler;
import mod.mh48.rageload.platform.Services48;

public class CompatHandler {


    public static void init() {
        if (Services48.PLATFORM.isModLoaded("lithostitched")) {
            LithostitchedCompatHandler.init();
            RageLoad.LOGGER.info("LithostitchedCompatHandler loaded");
        }
    }

    public static void preInit() {
        if (Services48.PLATFORM.isModLoaded("tectonic")) {
            try {
                TectonicCompatHandler.init();
            }catch (Throwable e){
                RageLoad.LOGGER.info("Loading tectonic compat failed:",e);
            }
        }
    }
}
