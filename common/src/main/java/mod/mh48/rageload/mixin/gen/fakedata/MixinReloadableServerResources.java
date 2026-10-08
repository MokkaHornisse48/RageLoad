package mod.mh48.rageload.mixin.gen.fakedata;

import com.mojang.brigadier.CommandDispatcher;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.client.worldgen.fakedata.ClientGenerationInitializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.ServerFunctionLibrary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ReloadableServerResources.class)
public class MixinReloadableServerResources {
    @Shadow
    @Final
    private CommandBuildContext.Configurable commandBuildContext;
    @Shadow
    @Final
    private Commands commands;

    @Redirect(method = "<init>", at = @At(value = "NEW", target  = "net/minecraft/commands/Commands") )
    public Commands modifyCommands(Commands.CommandSelection pSelection, CommandBuildContext pContext) {
        if (pSelection == ClientGenerationInitializer.NOCMDS) {
            try {
                return (Commands)  RageLoad.unsafe.allocateInstance(Commands.class);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
        return new Commands(pSelection, this.commandBuildContext);
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target  = "net/minecraft/server/ServerFunctionLibrary") )
    public ServerFunctionLibrary modifyServerFunctionLibrary(int pFunctionCompilationLevel, CommandDispatcher pDispatcher) {
        if (this.commands == null) {
            return null;
        }
        return new ServerFunctionLibrary(pFunctionCompilationLevel, this.commands.getDispatcher());
    }
}
