package mod.mh48.rageload.client.worldgen.fakedata;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.WorldGenData;
import mod.mh48.rageload.client.NetworkingClient;
import mod.mh48.rageload.client.worldgen.ClientChunkGenerator;
import mod.mh48.rageload.compat.tectonic.TectonicCompatHandler;
import mod.mh48.rageload.duck.gen.StructureTemplateManagerDuck;
import mod.mh48.rageload.platform.Services48;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static mod.mh48.rageload.WorldGenData.WorldGenRegistries;

public class ClientGenerationInitializer {
    public DynamicOps<Tag> ops = NbtOps.INSTANCE;

    public CompoundTag worldGenSettings;

    public static Commands.CommandSelection NOCMDS = Commands.CommandSelection.valueOf("NONE");
    public WorldOptions worldOptions;
    public FakeRegistry<Registry<?>> registries = new FakeRegistry<>(ResourceKey.createRegistryKey(BuiltInRegistries.ROOT_REGISTRY_NAME));


    public static HashMap<String, WorldGenData.RegistryWithCodec> decode = new HashMap<>(WorldGenRegistries.size());
    static {
        for (int i = 0; i < WorldGenRegistries.size(); i++) {
            ResourceKey k = WorldGenRegistries.get(i).registry();
            decode.put(k.location().toString(), WorldGenRegistries.get(i));
        }
    }


    public ClientGenerationInitializer(CompoundTag worldGenSettings) {
        this.worldGenSettings = worldGenSettings;

        setOpsRegistry(registries);
        CompoundTag extraTags = worldGenSettings.getCompound("extraTags");
        for(String key:extraTags.getAllKeys()){
            Registry<?> rw = Minecraft.getInstance().level.registryAccess().registryOrThrow(ResourceKey.createRegistryKey(new ResourceLocation(key)));
            tagsOnly(extraTags.getCompound(key),rw);
            registries.add(rw.key().location(),rw);
        }

        CompoundTag registriesData = worldGenSettings.getCompound("registries");
        for(String key:registriesData.getAllKeys()){
            WorldGenData.RegistryWithCodec rw = decode.get(key);
            registries.add(rw.registry().location(),new FakeRegistry<>(rw.registry()));
        }

        for (int i = 0; i < WorldGenRegistries.size(); i++) {
            ResourceKey<Registry<?>> key = WorldGenRegistries.get(i).registry();
            RageLoad.LOGGER.info("Fake registry: " + key);
            CompoundTag registryData = registriesData.getCompound(key.location().toString());
            WorldGenData.RegistryWithCodec rw = decode.get(key.location().toString());
            FakeRegistry<?> registry = (FakeRegistry<?>) registries.get(key.location());
            deserializeRegistry(registryData,rw.codec(),registry);
        }

        for(String key:extraTags.getAllKeys()){
            registries.remove(new ResourceLocation(key));
        }

        worldOptions = deserialize(worldGenSettings.get("WorldOptions"),WorldOptions.CODEC.codec());

        if(Services48.PLATFORM.isModLoaded("tectonic")){
            TectonicCompatHandler.loadConfig(ops,worldGenSettings.get("tectonicConfig"));

        }
    }

    public <T> void tagsOnly(CompoundTag rData, Registry<T> registry){
        for(String key:rData.getAllKeys()){
            HolderSet.Named<T> tag = registry.getOrCreateTag(TagKey.create(registry.key(), new ResourceLocation(key)));
            List<Holder<T>> holders = new ArrayList<>(tag.stream().toList());
            ListTag tagData = rData.getList(key,Tag.TAG_STRING);
            for(Tag t:tagData){
                String el = t.getAsString();
                if(!holders.stream().anyMatch(h->h.unwrapKey().get().location().toString().equals(el))) {
                    holders.add(registry.getHolder(ResourceKey.create(registry.key(), new ResourceLocation(el))).get());
                }
            }
            tag.bind(holders);
        }
    }

    public void setOpsRegistry(Registry<? extends Registry<?>> pRegistryOfRegistries){
        ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(pRegistryOfRegistries));
    }

    public <T> void deserializeRegistry(CompoundTag rData, Codec<T> codec, FakeRegistry<T> registry){
        CompoundTag tags = rData.getCompound("tags");
        for(String key:tags.getAllKeys()){
            HolderSet.Named<T> tag = registry.getOrCreateTag(TagKey.create(registry.key(), new ResourceLocation(key)));
            List<Holder<T>> holders = new ArrayList<>();
            ListTag tagData = tags.getList(key,Tag.TAG_STRING);
            for(Tag t:tagData){
                String el = t.getAsString();
                holders.add(registry.getHolder(ResourceKey.create(registry.key(),new ResourceLocation(el))).get());
            }
            tag.bind(holders);
        }

        CompoundTag values = rData.getCompound("values");
        for(String key:values.getAllKeys()){
            CompoundTag ellement = values.getCompound(key);
            T el = deserialize(ellement.get("el"),codec);
            registry.add(new ResourceLocation(key),el,ellement.getInt("id"));
        }
    }


    public <T> T deserialize(Tag nbt, Codec<T> codec) {
        return codec.parse(ops, nbt)
                .getOrThrow(false, error -> {
                    System.err.println("Deserialization failed: " + error);
                });
    }


    public ClientChunkGenerator initGenerator(){
        ClientGenServer server = createClientGenServer();
        registerOffloadedTemplates(server,worldGenSettings.getList("structureTemplates",Tag.TAG_STRING));
        return new ClientChunkGenerator(server,worldGenSettings);
    }


    public void registerOffloadedTemplates(MinecraftServer server, ListTag allTemplatesNbt) {
        StructureTemplateManager templateManager = server.getStructureManager();
        StructureTemplateManagerDuck accessor = (StructureTemplateManagerDuck) templateManager;

        List<ResourceLocation> structures = new ArrayList<>();
        for (Tag key : allTemplatesNbt) {
            structures.add(new ResourceLocation(key.getAsString()));
        }
        accessor.atrioffload$registerServerLoader(structures,(id)-> NetworkingClient.requestStructureData(id).join());
    }

    public ClientGenServer createClientGenServer() {

        try {
            Path rootPath = Files.createTempDirectory("mc_fake_server_");

            File file = rootPath.toFile();
            file.deleteOnExit();

            LevelStorageSource levelstoragesource = new EmptyLevelStorageSource(rootPath,DataFixers.getDataFixer());
            LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.validateAndCreateAccess("");
            PackRepository packrepository = new PackRepository();
            ArrayList<RegistryAccess.Frozen> rs = new ArrayList<>();


            //stemRegistry.entires.put()

            rs.add(new FakeRegistryAccess(
                    registries.getEntries()
            ));



            LayeredRegistryAccess<RegistryLayer> registries = RegistryLayer.createRegistryAccess().replaceFrom(RegistryLayer.RELOADABLE,rs);


            WorldStem worldstem = new WorldStem(new FakeResourceManager(),new ReloadableServerResources(registries.compositeAccess(),FeatureFlagSet.of(), NOCMDS,0),registries, createPrimaryLevelData(worldOptions));


            final ClientGenServer dedicatedserver = MinecraftServer.spin((p_129697_) -> {
                ClientGenServer dedicatedserver1 = new ClientGenServer(rootPath, p_129697_, levelstoragesource$levelstorageaccess, packrepository, worldstem, SilenceProgress::new);
                dedicatedserver1.setSingleplayerProfile(null);
                dedicatedserver1.setPort(-1);
                dedicatedserver1.setDemo(false);
                return dedicatedserver1;
            });
            return dedicatedserver;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static class SilenceProgress implements ChunkProgressListener {

        public SilenceProgress(int i) {}

        @Override
        public void onStatusChange(ChunkPos pChunkPosition, @Nullable ChunkStatus pNewStatus) {
        }

        //Should not get called (:
        @Override
        public void updateSpawnPos(ChunkPos pCenter) {}
        @Override
        public void start() {}
        @Override
        public void stop() {}
    }


    public static class FakeRegistryAccess implements RegistryAccess.Frozen{

        public final List<RegistryEntry<?>> registries;

        public FakeRegistryAccess(List<RegistryEntry<?>> registries) {
            this.registries = registries;
        }

        public FakeRegistryAccess(RegistryEntry<?> ...pValues) {
            this(Arrays.asList(pValues));
        }

        @Override
        public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
            for(RegistryEntry<?> entry:registries){
                if(entry.key().equals(pRegistryKey))return Optional.of((Registry<E>) entry.value());
            }
            return Optional.empty();
        }

        @Override
        public Stream<RegistryEntry<?>> registries() {
            return registries.stream();
        }
    }

    public PrimaryLevelData createPrimaryLevelData(WorldOptions worldOptions){
        LevelSettings settings = new LevelSettings("cwg",GameType.CREATIVE,false,Difficulty.EASY,false,new GameRules(),WorldDataConfiguration.DEFAULT);
        return new PrimaryLevelData(settings,worldOptions, PrimaryLevelData.SpecialWorldProperty.NONE,Lifecycle.stable());
    }


    public static class FakeResourceManager implements CloseableResourceManager {

        @Override
        public Set<String> getNamespaces() {
            return Set.of();
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation pLocation) {
            return List.of();
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String pPath, Predicate<ResourceLocation> pFilter) {
            return Map.of();
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String pPath, Predicate<ResourceLocation> pFilter) {
            return Map.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.empty();
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation pLocation) {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    }

}
