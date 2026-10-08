package mod.mh48.rageload.duck.gen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.function.Function;

public interface StructureTemplateManagerDuck {
    @Unique
    void atrioffload$registerServerLoader(List<ResourceLocation> serverStructures, Function<ResourceLocation, CompoundTag> loadStructure);
}
