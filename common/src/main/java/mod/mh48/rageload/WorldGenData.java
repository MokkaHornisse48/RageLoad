package mod.mh48.rageload;

import com.mojang.datafixers.util.Pair;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import mod.mh48.rageload.compat.tectonic.TectonicCompatHandler;
import mod.mh48.rageload.duck.server.RegistryOpsDuck;
import mod.mh48.rageload.platform.Services48;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.Map;

public class WorldGenData {
    public record RegistryWithCodec<T>(ResourceKey<Registry<T>> registry,Codec<T> codec){}

    public static ArrayList<RegistryWithCodec> WorldGenRegistries = new ArrayList<>();

    public static <T> void rc(ResourceKey<Registry<T>> registry,Codec<T> codec){
        WorldGenRegistries.add(new RegistryWithCodec<>(registry,codec));
    }

    static {
        rc(Registries.PROCESSOR_LIST,StructureProcessorType.DIRECT_CODEC);
        rc(Registries.CONFIGURED_FEATURE,ConfiguredFeature.DIRECT_CODEC);
        rc(Registries.PLACED_FEATURE,PlacedFeature.DIRECT_CODEC);
        rc(Registries.CONFIGURED_CARVER,ConfiguredWorldCarver.DIRECT_CODEC);
        rc(Registries.BIOME,Biome.DIRECT_CODEC);
        rc(Registries.NOISE,NormalNoise.NoiseParameters.DIRECT_CODEC);
        rc(Registries.DENSITY_FUNCTION,DensityFunction.DIRECT_CODEC);
        rc(Registries.NOISE_SETTINGS,NoiseGeneratorSettings.DIRECT_CODEC);
        rc(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,MultiNoiseBiomeSourceParameterList.DIRECT_CODEC);
        rc(Registries.DIMENSION_TYPE,DimensionType.DIRECT_CODEC);
        rc(Registries.LEVEL_STEM,LevelStem.CODEC);
        rc(Registries.DAMAGE_TYPE,DamageType.CODEC);
        rc(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC);
        rc(Registries.TEMPLATE_POOL, StructureTemplatePool.DIRECT_CODEC);
        rc(Registries.STRUCTURE, Structure.DIRECT_CODEC);
    }

    /*
    * public static final ResourceKey<Registry<StructurePieceType>> STRUCTURE_PIECE = createRegistryKey("worldgen/structure_piece");
    public static final ResourceKey<Registry<StructurePlacementType<?>>> STRUCTURE_PLACEMENT = createRegistryKey("worldgen/structure_placement");
    public static final ResourceKey<Registry<StructurePoolElementType<?>>> STRUCTURE_POOL_ELEMENT = createRegistryKey("worldgen/structure_pool_element");
    public static final ResourceKey<Registry<StructureProcessorType<?>>> STRUCTURE_PROCESSOR = createRegistryKey("worldgen/structure_processor");
    public static final ResourceKey<Registry<StructureType<?>>> STRUCTURE_TYPE = createRegistryKey("worldgen/structure_type");
    public static final ResourceKey<Registry<TreeDecoratorType<?>>> TREE_DECORATOR_TYPE = createRegistryKey("worldgen/tree_decorator_type");
    public static final ResourceKey<Registry<TrunkPlacerType<?>>> TRUNK_PLACER_TYPE = createRegistryKey("worldgen/trunk_placer_type");
    public static final ResourceKey<Registry<PlacedFeature>> PLACED_FEATURE = createRegistryKey("worldgen/placed_feature");
    public static final ResourceKey<Registry<StructureTemplatePool>> TEMPLATE_POOL = createRegistryKey("worldgen/template_pool");
    * */

    public static ResourceKey<Registry<?>>[] extraTagRegisties = new ResourceKey[]{
            Registries.BLOCK,
            Registries.FLUID,
            Registries.SOUND_EVENT
    };


    public static CompoundTag generateWorldGenData(MinecraftServer server){
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());

        ((RegistryOpsDuck)ops).setForGenData();

        CompoundTag data = new CompoundTag();
        data.put("WorldOptions", serialize(server.getWorldData().worldGenOptions(), WorldOptions.CODEC.codec(),ops));
        CompoundTag registries = new CompoundTag();

        for (int i = 0; i < WorldGenRegistries.size(); i++) {
            ResourceKey<Registry<?>> k = WorldGenRegistries.get(i).registry;
            Registry<?> registry = server.registries().compositeAccess().registryOrThrow(k);
            registries.put(k.location().toString(),serializeRegistry(registry,WorldGenRegistries.get(i).codec,ops));
        }
        data.put("registries",registries);

        CompoundTag extraTags = new CompoundTag();

        for (int i = 0; i < extraTagRegisties.length; i++) {
            ResourceKey<Registry<?>> k = extraTagRegisties[i];
            Registry<?> registry = server.registries().compositeAccess().registryOrThrow(k);
            extraTags.put(k.location().toString(),serializeTagsOnly(registry));
        }

        data.put("extraTags",extraTags);

        data.put("structureTemplates",serializeAllTemplates(server));

        if(Services48.PLATFORM.isModLoaded("tectonic")){
            Tag tc = TectonicCompatHandler.getConfig(ops);
            data.put("tectonicConfig",tc);
        }

        return data;
    }

    public static <T> CompoundTag serializeRegistry(Registry<T> registry, Codec<T> codec,RegistryOps<Tag> ops){
        CompoundTag rData = new CompoundTag();
        CompoundTag values = new CompoundTag();
        for(Map.Entry<ResourceKey<T>,T> entry:registry.entrySet()){
            Tag el = serialize(entry.getValue(),codec,ops);
            String key = entry.getKey().location().toString();
            CompoundTag element = new CompoundTag();
            element.putInt("id",registry.getId(entry.getValue()));
            element.put("el",el);
            values.put(key,element);
        }
        rData.put("values",values);
        CompoundTag tags = new CompoundTag();
        for(Pair<TagKey<T>, HolderSet.Named<T>> tag:registry.getTags().toList()){
            ListTag tagData = new ListTag();
            for(Holder<T> h:tag.getSecond().stream().toList()){
                tagData.add(StringTag.valueOf(h.unwrapKey().get().location().toString()));
            }
            tags.put(tag.getFirst().location().toString(),tagData);
        }
        rData.put("tags",tags);
        return rData;
    }

    public static <T> CompoundTag serializeTagsOnly(Registry<T> registry){
        CompoundTag rData = new CompoundTag();
        for(Pair<TagKey<T>, HolderSet.Named<T>> tag:registry.getTags().toList()){
            ListTag tagData = new ListTag();
            for(Holder<T> h:tag.getSecond().stream().toList()){
                tagData.add(StringTag.valueOf(h.unwrapKey().get().location().toString()));
            }
            rData.put(tag.getFirst().location().toString(),tagData);
        }
        return rData;
    }

    public static <T> Tag serialize(T object, Codec<T> codec, DynamicOps<Tag> ops){
        return codec.encodeStart(ops, object)
                .getOrThrow(false, error -> {
                    System.err.println("Serialization failed: " + error);
                });
    }

    public static ListTag serializeAllTemplates(MinecraftServer server) {
        StructureTemplateManager templateManager = server.getStructureManager();
        ListTag allTemplates = new ListTag();

        for(ResourceLocation loc:templateManager.listTemplates().toList()){
            allTemplates.add(StringTag.valueOf(loc.toString()));
        }

        return allTemplates;
    }


}
