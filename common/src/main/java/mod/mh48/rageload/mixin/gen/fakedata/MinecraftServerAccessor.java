package mod.mh48.rageload.mixin.gen.fakedata;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

    @Accessor("delayedTasksMaxNextTickTime")
    void setDelayedTasksMaxNextTickTime(long l);

    @Accessor("nextTickTime")
    void setNextTickTime(long l);
}
