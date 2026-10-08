package mod.mh48.rageload.mixin.gen.fakedata;

import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Commands.CommandSelection.class)
public abstract class MixinCommandSelection {

    // Wir erstellen den neuen Enum-Eintrag "NONE"
    @Unique
    private static final Commands.CommandSelection NONE = mixin$createNONE("NONE", -1, false, false);

    // Hilfsmethode, um den Konstruktor aufzurufen
    @Invoker("<init>")
    public static Commands.CommandSelection mixin$createNONE(String name, int ordinal, boolean integrated, boolean dedicated) {
        throw new IllegalStateException("Mixin did not apply!");
    }

    // Wir fügen den neuen Wert in das interne Array ein, damit values() ihn findet
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mixin$addCustomElement(CallbackInfo ci) {
        Commands.CommandSelection[] values = Commands.CommandSelection.values();
        Commands.CommandSelection[] newValues = Arrays.copyOf(values, values.length + 1);
        newValues[newValues.length - 1] = NONE;

        // Hier setzen wir das interne $VALUES Feld neu (Shadowing erforderlich)
        setValues(newValues);
    }

    @Shadow
    @Mutable
    @Final
    private static Commands.CommandSelection[] $VALUES;

    @Unique
    private static void setValues(Commands.CommandSelection[] newValues) {
        $VALUES = newValues;
    }
}
