package mod.mh48.rageload.client;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.client.worldgen.ClientChunkGenerator;
import mod.mh48.rageload.client.worldgen.SaveProtoChunk;
import mod.mh48.rageload.duck.client.ClientPacketListenerDuck;
import mod.mh48.rageload.duck.gen.LevelChunkDuck;
import mod.mh48.rageload.duck.gen.ProtoChunkDuck;
import mod.mh48.rageload.mixin.client.ClientChunkCacheStorageAccessor;
import mod.mh48.rageload.mixin.client.ClientLevelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ClientWorldLoader implements AutoCloseable {


    public final ClientLevel level;

    public final Supplier<ClientChunkCache.Storage> storage;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread loadUnloaded;
    private final Thread genUnloaded;
    private final ClientPacketListenerDuck genGetter;

    public ClientWorldLoader(ClientLevel level, Supplier<ClientChunkCache.Storage> storage) {
        this.level = level;
        this.storage = storage;
        loadUnloaded = new Thread(this::loop, "ClientWorldLoader");
        //loadUnloaded.setPriority(3);
        loadUnloaded.start();

        genUnloaded = new Thread(this::loopGen, "ClientWorldGenerator");
        //genUnloaded.setPriority(3);
        genUnloaded.start();
        genGetter = (ClientPacketListenerDuck)((ClientLevelAccessor)level).getConnection();

    }

    private void loop() {
        try {

            while (running.get()) {
                if (running.get()) {
                    this.loadUnloaded();
                }
            }
        } catch (InterruptedException e) {
        }
        RageLoad.LOGGER.info("Stopped ClientWorldLoader");
    }

    private void loopGen() {
        try {
        while (running.get()) {
            if (running.get()) {
                this.checkGenning();
            }
        }
        } catch (InterruptedException e) {
        }
        RageLoad.LOGGER.info("Stopped ClientWorldGenerator");
    }

    @Override
    public void close() {
        running.set(false);

        loadUnloaded.interrupt();
        genUnloaded.interrupt();
    }

    public static long pack(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    public CompoundTag getChunk(int x,int z,byte[] chunkdata,byte compression) {
        try {
            InputStream in;
            if (compression == 2) {
                in = new InflaterInputStream(new ByteArrayInputStream(chunkdata));
            } else if (compression == 1) {
                in = new GZIPInputStream(new ByteArrayInputStream(chunkdata));
            } else {
                return null;
            }
            CompoundTag nbt = NbtIo.read(new DataInputStream(in));
            return nbt;
        } catch (Exception e) {
            RageLoad.LOGGER.info("Failed to decrypt chunk at ["+x+" "+z+"] \n",e);
            return null;
        }
    }

    //public ArrayBlockingQueue<LevelChunk> chunks = new ArrayBlockingQueue<>(1000);

    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());

    public FriendlyByteBuf createPacket(int x,int z,CompoundTag chunk){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(x);
        buf.writeInt(z);

        if(ChunkStatus.byName(chunk.getString("Status"))!= ChunkStatus.FULL)return null;

        //chunk data
        buf.writeNbt(chunk.getCompound("Heightmaps"));
        int sectionsCount = level.getSectionsCount();
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = makeBiomeCodec(biomeRegistry);
        LevelChunkSection[] sections = new LevelChunkSection[sectionsCount];
        ListTag sectionsNbt = chunk.getList("sections", CompoundTag.TAG_COMPOUND);

        ArrayList<byte[]> skyLight = new ArrayList<>(Collections.nCopies(sectionsCount, null));
        ArrayList<byte[]> blockLight = new ArrayList<>(Collections.nCopies(sectionsCount, null));

        for(int j = 0; j < sectionsNbt.size(); ++j) {
            CompoundTag compoundtag = sectionsNbt.getCompound(j);
            int k = compoundtag.getByte("Y");
            int l = level.getSectionIndexFromSectionY(k);
            if (l >= 0 && l < sectionsCount) {
                PalettedContainer<BlockState> palettedcontainer;
                if (compoundtag.contains("block_states", 10)) {
                    palettedcontainer = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, compoundtag.getCompound("block_states")).promotePartial((p_188283_) -> {
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedcontainer = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
                }
                PalettedContainerRO<Holder<Biome>> palettedcontainerro;
                if (compoundtag.contains("biomes", 10)) {
                    palettedcontainerro = biomeCodec.parse(NbtOps.INSTANCE, compoundtag.getCompound("biomes")).promotePartial((p_188274_) -> {
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedcontainerro = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
                }

                LevelChunkSection levelchunksection = new LevelChunkSection(palettedcontainer, palettedcontainerro);
                sections[l] = levelchunksection;

                if(compoundtag.contains("BlockLight", 7)){
                    blockLight.set(l,compoundtag.getByteArray("BlockLight"));
                }
                if(compoundtag.contains("SkyLight", 7)){
                    skyLight.set(l,compoundtag.getByteArray("SkyLight"));
                }
            }
        }
        int chunkSize = 0;
        for(int j = 0; j < sections.length; ++j) {
            if (sections[j] == null) sections[j] = new LevelChunkSection(
                    new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES),
                    new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES));
            chunkSize += sections[j].getSerializedSize();
        }
        FriendlyByteBuf chunkdata = new FriendlyByteBuf(Unpooled.buffer(chunkSize));
        for(int j = 0; j < sections.length; ++j) {
            sections[j].write(chunkdata);
        }
        buf.writeVarInt(chunkdata.readableBytes());
        buf.writeBytes(chunkdata);

        ListTag blockEntities = chunk.getList("block_entities", CompoundTag.TAG_COMPOUND);
        List<Tag> filtered = blockEntities.stream()
                .filter(be -> BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.tryParse(((CompoundTag)be).getString("id"))))
                .toList();
        buf.writeVarInt(filtered.size());
        for (Tag be : filtered) {
            CompoundTag blockEntity = (CompoundTag) be.copy();
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.tryParse(blockEntity.getString("id")));

            int ex = blockEntity.getInt("x");
            int ey = blockEntity.getInt("y");
            int ez = blockEntity.getInt("z");
            int exz = SectionPos.sectionRelative(ex) << 4 | SectionPos.sectionRelative(ez);
            blockEntity.remove("x");
            blockEntity.remove("y");
            blockEntity.remove("z");
            blockEntity.remove("id");
            buf.writeByte(exz);
            buf.writeShort(ey);
            buf.writeId(BuiltInRegistries.BLOCK_ENTITY_TYPE, type);
            buf.writeNbt(blockEntity);

        }

        //light data
        BitSet skyYMask = new BitSet();
        BitSet blockYMask = new BitSet();
        BitSet emptySkyYMask = new BitSet();
        BitSet emptyBlockYMask = new BitSet();
        for(int j = 0; j < sections.length; ++j) {//todo medium prio make cleaner by using light engine sections.
            if(skyLight.get(j)==null) {
                emptySkyYMask.set(j+1);
            }else{
                skyYMask.set(j+1);
            }
            if(blockLight.get(j)==null) {
                emptyBlockYMask.set(j+1);
            }else{
                blockYMask.set(j+1);
            }
        }
        buf.writeBitSet(skyYMask);
        buf.writeBitSet(blockYMask);
        buf.writeBitSet(emptySkyYMask);
        buf.writeBitSet(emptyBlockYMask);
        skyLight.removeIf(Objects::isNull);
        blockLight.removeIf(Objects::isNull);

        buf.writeCollection(skyLight, FriendlyByteBuf::writeByteArray);
        buf.writeCollection(blockLight, FriendlyByteBuf::writeByteArray);

        return buf;
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> pBiomeRegistry) {
        return PalettedContainer.codecRO(pBiomeRegistry.asHolderIdMap(), pBiomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, pBiomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }


    private void loadChunk(ClientboundLevelChunkWithLightPacket chunkData,boolean generated){
        Minecraft.getInstance().execute(
                () -> innerLoadChunk(chunkData,generated)
        );
    }

    public LinkedBlockingQueue<SaveProtoChunk> clientChunkOverwrite = new LinkedBlockingQueue<>();

    private void innerLoadChunk(ClientboundLevelChunkWithLightPacket chunkData,boolean generated){
        int x = chunkData.getX();
        int z = chunkData.getZ();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener handler = mc.getConnection(); // must be non-null
        if (handler != null) {
            LevelChunk chunk = mc.level.getChunkSource().getChunk(x, z, false);
            if(chunk==null || ((LevelChunkDuck)chunk).wasGeneratedByClient()) {
                handler.handleLevelChunkWithLight(chunkData);
                /*
                LevelChunk chunk = mc.level.getChunkSource().getChunk(x, z, false);
                this.level.onChunkLoaded(new ChunkPos(x, z));
                this.level.getLightEngine().setLightEnabled(chunk.getPos(), true);
                chunk.setLightCorrect(true);
                for(int k = this.level.getMinSection(); k < this.level.getMaxSection(); ++k) {
                    mc.levelRenderer.setSectionDirty(x, k, z);
                }*/

                chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if(chunk!=null) {
                    ((LevelChunkDuck)chunk).setGeneratedByClient(generated);
                    if(genGetter.getGenerator()!=null && generated) {
                        SaveProtoChunk save = new SaveProtoChunk(chunk,genGetter.getGenerator().server.registryAccess().registryOrThrow(Registries.BIOME));
                        clientChunkOverwrite.add(save);
                        ((LevelChunkDuck)chunk).setSave(save);
                    }
                    //Atrioffload.LOGGER.info("Loaded chunk at ["+x+" "+z+"]");
                }
            }
        }

    }

    private boolean tryChunk(ClientChunkData chunkWD) {
        int x = chunkWD.x;
        int z = chunkWD.z;
        //Atrioffload.LOGGER.info("Loading chunk at ["+x+" "+z+"]");
        if(chunkWD.compression<=0||chunkWD.data.length<=0){
            return false;
        }
        CompoundTag chunk = getChunk(x,z,chunkWD.data, chunkWD.compression);
        if(chunk==null)return false;
        //load chunk
        try {
            FriendlyByteBuf p = this.createPacket(x, z, chunk);
            if(p==null) return false;

            ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(p);
            loadChunk(packet,false);
            return true;
        } catch (Exception e) {
            RageLoad.LOGGER.info("Failed to encode chunk at ["+x+" "+z+"] \n",e);
        }
        return false;
    }


    public record ClientChunkData(int x,int z,byte compression,byte[] data){};

    public LinkedBlockingQueue<ClientChunkData> queue = new LinkedBlockingQueue<>();

    public void loadUnloaded() throws InterruptedException {
        ClientChunkData chunk = queue.take();
        if(!tryChunk(chunk)){
        }
    }

    private Long2ObjectOpenHashMap<ProtoChunk> genningChunks = new Long2ObjectOpenHashMap<>();

    public boolean inRange(int pX, int pZ,ClientChunkCacheStorageAccessor sa,int extended) {
        return Math.abs(pX - sa.getViewCenterX()) <= sa.getChunkRadius() + extended && Math.abs(pZ - sa.getViewCenterZ()) <= sa.getChunkRadius() + extended;
    }


    public ProtoChunk getGenningChunk(ChunkPos pos){
        ProtoChunk chunk;
        long posl = pack(pos.x,pos.z);
        if(genningChunks.containsKey(posl)) {
            return genningChunks.get(posl);
        }
        // I know this would be better but this causes so many threading problems that this is no longer a thing for now.
        LevelChunk lchunk = level.getChunkSource().getChunkNow(pos.x, pos.z);

        chunk = new ProtoChunk(pos,UpgradeData.EMPTY, level, genGetter.getGenerator().server.registryAccess().registryOrThrow(Registries.BIOME), null);
        genningChunks.put(posl,chunk);
        return chunk;
    }

    public void tick() {
        ClientChunkCacheStorageAccessor sa = ((ClientChunkCacheStorageAccessor)(Object) storage.get());
        AtomicReferenceArray<LevelChunk> chunks = sa.getChunks();
        for (int i = 0; i < chunks.length(); i++) {
            LevelChunk ch = chunks.get(i);
            if(ch==null)continue;
            SaveProtoChunk save = ((LevelChunkDuck) ch).getSave();
            if(save==null)continue;
            save.merge();
        }
    }


    public void checkGenning() throws InterruptedException {
        ClientChunkGenerator gen = genGetter.getGenerator();
        if(gen == null || gen.server == null)return;
        ClientChunkCacheStorageAccessor sa = ((ClientChunkCacheStorageAccessor)(Object) storage.get());
        genningChunks.long2ObjectEntrySet().removeIf(pc -> !inRange(unpackX(pc.getLongKey()),unpackZ(pc.getLongKey()),sa,10) || (pc.getValue() instanceof SaveProtoChunk save && level.getChunkSource().getChunk(unpackX(pc.getLongKey()),unpackZ(pc.getLongKey()),false) != save.getWrapped()));


        //||(level.getChunkSource().hasChunk(unpackX(pc.getKey()),unpackZ(pc.getKey())) && !(pc.getValue() instanceof FilledProtoChunk)));
        while (!clientChunkOverwrite.isEmpty()){
            SaveProtoChunk save = clientChunkOverwrite.poll();
            long lpos = pack(save.getPos().x,save.getPos().z);
            if(genningChunks.containsKey(lpos)){
                ProtoChunk old = genningChunks.get(lpos);
                save.setAllStarts(old.getAllStarts());
                save.setAllReferences(old.getAllReferences());
                save.setStatus(old.getStatus());
            }
            genningChunks.put(lpos,save);
        }
        ChunkPos pos = getClosestChunk();
        if(pos==null){
            Thread.sleep(20);
            //Atrioffload.LOGGER.info("SLEEP");
            return;
        }
        gen.generateChunk(this.level.dimension(),pos,this::getGenningChunk,(chunk,level)->{
            ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(new LevelChunk(level,chunk, null), gen.server.getLevel(level.dimension()).getLightEngine(), new BitSet(), new BitSet());
            ClientboundLightUpdatePacketData ld = packet.getLightData();
            fillLight(((ProtoChunkDuck)chunk).optiload$getLightSky(),ld.getSkyYMask(),ld.getEmptySkyYMask(),ld.getSkyUpdates());
            fillLight(((ProtoChunkDuck)chunk).optiload$getLightBlock(),ld.getBlockYMask(),ld.getEmptyBlockYMask(),ld.getBlockUpdates());
            loadChunk(packet,true);
            ((ProtoChunkDuck)chunk).optiload$setLoaded();
            //Atrioffload.LOGGER.info("Generated chunk at ["+pos+"]");
        });
    }

    public void fillLight(DataLayer[] data,BitSet present,BitSet empty,List<byte[]> list){
        if(data==null){//LOL

            return;
        }
        for (int i = 0; i < data.length; i++) {
            if (data[i]==null || data[i].isEmpty()) {
                empty.set(i);
            } else {
                present.set(i);
                list.add(data[i].getData());
            }
        }
    }

    public ChunkPos getClosestChunk() {
        if (Minecraft.getInstance().player == null) return null;
        // Get player's current chunk coordinates
        ClientChunkCacheStorageAccessor sa = ((ClientChunkCacheStorageAccessor)(Object) storage.get());
        int distance = sa.getChunkRadius();
        int centerX = sa.getViewCenterX();
        int centerZ = sa.getViewCenterZ();


        for (int r = 0; r <= distance ; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) == r) {
                        int cx = centerX + x;
                        int cz = centerZ + z;
                        if (level.getChunkSource().hasChunk(cx,cz)) continue;
                        if (genningChunks.containsKey(pack(cx,cz))) {
                            ProtoChunk pch = genningChunks.get(pack(cx,cz));
                            if (pch != null && ((ProtoChunkDuck)pch).optiload$isWaitingGen()) continue;
                            if (pch instanceof SaveProtoChunk) continue;
                            if(pch != null && ((ProtoChunkDuck)pch).optiload$gotLoaded())continue;
                        }
                        return new ChunkPos(cx,cz);
                    }
                }
            }
        }
        return null;
    }


}