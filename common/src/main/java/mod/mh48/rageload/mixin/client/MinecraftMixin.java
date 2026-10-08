package mod.mh48.rageload.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public ClientLevel level;

    @Inject(method="setLevel",at = @At("HEAD"))
    public void onSetLevel(ClientLevel pLevelClient, CallbackInfo ci){
        try {
            if(this.level != null){
                ((AutoCloseable)this.level.getChunkSource()).close();
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method="clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V",at = @At("HEAD"))
    public void onClearLevel(Screen pScreen, CallbackInfo ci){
        try {
            if(this.level != null){
                ((AutoCloseable)this.level.getChunkSource()).close();
            }
        } catch (Exception ignored) {
        }
    }
}
