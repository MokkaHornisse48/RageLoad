package mod.mh48.rageload.mixin.gen.fakedata;

import com.google.common.collect.ImmutableList;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.duck.gen.StructureTemplateManagerDuck;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(StructureTemplateManager.class)
public abstract class StructureTemplateManagerMixin implements StructureTemplateManagerDuck {

    @Shadow
    public abstract StructureTemplate readStructure(CompoundTag arg);

    @Unique
    public List<ResourceLocation> atrioffload$serverStructures;

    @Unique
    public Function<ResourceLocation,CompoundTag> atrioffload$loadStructure;


    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<StructureTemplateManager.Source> addAtriOffloadSource(ImmutableList.Builder<StructureTemplateManager.Source> builder) {
        builder.add(new StructureTemplateManager.Source(this::atrioffload$loadFromServer, () -> atrioffload$listFromServer()));
        return builder.build();
    }

    @Unique
    private Optional<StructureTemplate> atrioffload$loadFromServer(ResourceLocation id) {
        if(atrioffload$loadStructure==null)return Optional.empty();
        try {
            CompoundTag nbt = atrioffload$loadStructure.apply(id);
            if (nbt != null) {
                return Optional.of(this.readStructure(nbt));
            }
        } catch (Exception e) {
            RageLoad.LOGGER.error("Failed to offload structure " + id, e);
        }

        return Optional.empty();
    }

    @Unique
    private Stream<ResourceLocation> atrioffload$listFromServer() {
        if(atrioffload$serverStructures==null)return Stream.empty();
        return atrioffload$serverStructures.stream();
    }

    @Unique
    @Override
    public void atrioffload$registerServerLoader(List<ResourceLocation> serverStructures, Function<ResourceLocation, CompoundTag> loadStructure){
        this.atrioffload$serverStructures = serverStructures;
        this.atrioffload$loadStructure = loadStructure;
    }
}
