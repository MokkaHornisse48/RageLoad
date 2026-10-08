package mod.mh48.rageload.mixin.server;

import mod.mh48.rageload.duck.server.RegistryOpsDuck;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RegistryOps.class)
public class RegistryOpsMixin implements RegistryOpsDuck {
    @Unique
    public boolean forGenData = false;

    @Override
    public void setForGenData() {
        forGenData = true;
    }

    @Override
    public boolean isForGenData() {
        return forGenData;
    }
}
