package mod.mh48.rageload;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import mod.mh48.rageload.mixin.server.ChunkMapAccessor;
import mod.mh48.rageload.mixin.server.MinecraftServerAccessor;
import mod.mh48.rageload.net.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

public class Networking {

    public static final ResourceLocation REQUEST_GEN_DATA_PACKET = RageLoad.of( "request_gen_data");

    public static final ResourceLocation INIT_WORLD_DATA_PACKET = RageLoad.of( "init_world_data");

    public static final ResourceLocation GEN_DATA_PACKET = RageLoad.of( "gen_data");

    public static final ResourceLocation CHUNK_DATA_PACKET = RageLoad.of( "chunk_data");

    public static final ResourceLocation GET_STRUCTURE_DATA = RageLoad.of( "get_structure_data");

    public static final ResourceLocation STRUCTURE_DATA = RageLoad.of( "structure_data");

    public static void init(){
        NetworkManager.registerReceiver( REQUEST_GEN_DATA_PACKET, (ebuf, player) -> {
            sendGenData(player,player.getServer());
        });

        NetworkManager.registerReceiver( INIT_WORLD_DATA_PACKET, (buf, player) -> {
            int distance = buf.readInt();
            if(senders.containsKey(player.getUUID())){
                senders.get(player.getUUID()).setDistance(distance);
            }else{
                senders.put(player.getUUID(),new DataSender(distance));
            }
            System.out.println("Initialized world data "+player+" "+distance);
        });

        NetworkManager.registerReceiver(GET_STRUCTURE_DATA, (buf, player) -> {
            ResourceLocation loc = buf.readResourceLocation();
            StructureTemplateManager templateManager = player.getServer().getStructureManager();
            Optional<StructureTemplate> template = templateManager.get(loc);
            if(template.isPresent()){
                sendStructureData(player,loc,template.get());
            }else{
                sendStructureData(player,loc,null);
            }
        });

        //TickEvent.SERVER_POST.register(Networking::tick); now in mixin net.MinecraftServerMixin
    }

    public static void sendStructureData(ServerPlayer player, ResourceLocation loc,StructureTemplate template) {
        if (!player.connection.isAcceptingMessages()) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(loc);
        if(template!=null) {
            buf.writeNbt(template.save(new CompoundTag()));
        }else{
            buf.writeNbt(null);
        }
        NetworkManager.sendToPlayer(player, STRUCTURE_DATA, buf);
    }

    private static boolean sendChunk(ServerPlayer player, int x, int z) {
        ServerLevel level = (ServerLevel) player.level();

        byte[] fullData = null;
        byte compression = 0;

        ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk != null) {
            CompoundTag compoundTag = ChunkSerializer.write(level, chunk);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflaterOut = new DeflaterOutputStream(baos);
                 DataOutputStream dos = new DataOutputStream(deflaterOut)) {

                NbtIo.write(compoundTag, dos);
                deflaterOut.finish();
                fullData = baos.toByteArray();
                compression = 2;

            } catch (IOException e) {
                RageLoad.LOGGER.error("Fehler beim Serialisieren des Chunks: ", e);
            }
        }
        if(fullData==null){
            LevelStorageSource.LevelStorageAccess access = ((MinecraftServerAccessor) player.getServer()).getStorageSource();
            Path dimensionPath = access.getDimensionPath(level.dimension());
            Path regionFolder = dimensionPath.resolve("region");
            Path regionFile = regionFolder.resolve("r."+getRegion(x)+"."+getRegion(z)+".mca");
            try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)){
                MappedByteBuffer r = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                if(r.hasRemaining()) {
                    int oi = getRegionLocal(x) + getRegionLocal(z) * 32;
                    int entry = r.getInt(oi * 4);

                    int offset = (entry >> 8) & 0xFFFFFF;
                    int sectorCount = entry & 0xFF;
                    if (offset != 0) {
                        int sectorOffset = offset * 4096;
                        int length = r.getInt(sectorOffset);
                        compression = r.get(sectorOffset + 4);

                        fullData = new byte[length - 1];
                        r.position(sectorOffset + 5);
                        r.get(fullData);
                    }
                }
            } catch (IOException e) {
            }
        }


        if(fullData!=null) {
            sendChunkData(player, x,z, compression, fullData);
            return true;
        }else{
            //sendChunkData(player,x,z, (byte) 0,new byte[0]); I do not need to send empty chunks. This was needed once to initialize chunk generation, but I now use a different system.
            return false;
        }
    }

    public static int getRegion(int v) {
        return v >> 5;
    }

    public static int getRegionLocal(int v) {
        return v & 31;
    }

    public static byte[] genData = null;

    public static void initGenData(MinecraftServer server){
        long startTime = System.nanoTime();
        CompoundTag data = WorldGenData.generateWorldGenData(server);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream deflaterOut = new DeflaterOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(deflaterOut)) {

            NbtIo.write(data, dos);
            deflaterOut.finish();
            genData = baos.toByteArray();

            long endTime = System.nanoTime(); // Endzeit nach der Kompression
            long durationMs = (endTime - startTime) / 1000000; // Umrechnung in ms
            RageLoad.LOGGER.info("Generated and compressed WorldGenData in {} ms (Size: {} KB)",
                    durationMs, genData.length / 1024);
        } catch (IOException e) {
            RageLoad.LOGGER.error("Fehler beim Serialisieren der GenData NBT: ", e);
        }
    }

    public static void sendGenData(ServerPlayer player, MinecraftServer server) {
        if (!player.connection.isAcceptingMessages()) return;

        if(genData == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(genData.length);
        buf.writeBytes(genData);
        NetworkManager.sendToPlayer(player, GEN_DATA_PACKET, buf);
    }


    public static void tick(MinecraftServer server) {

        Iterator<Map.Entry<UUID, DataSender>> iterator = senders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DataSender> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            entry.getValue().tick(player);
        }
    }

    public static boolean sendChunkData(ServerPlayer player, int x,int z,byte compression,byte[] data) {
        if (!player.connection.isAcceptingMessages()) return false;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(x);
        buf.writeInt(z);
        buf.writeByte(compression);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        NetworkManager.sendToPlayer(player, CHUNK_DATA_PACKET, buf);
        return true;
    }

    public static HashMap<UUID,DataSender> senders = new HashMap<>();

    public static class DataSender{
        private int distance;
        private LongOpenHashSet sendChunks = new LongOpenHashSet();

        public DataSender(int distance) {
            this.distance = distance;
        }

        public void setDistance(int distance){
            this.distance = distance;
        }

        public void tick(ServerPlayer player){
            int centerX = player.getLastSectionPos().x();
            int centerZ = player.getLastSectionPos().z();
            sendChunks.removeIf((ch)->!inRange(centerX,centerZ,ch,distance));

            int chunksPerTick = 2;
            int sentThisTick = 0;
            int viewDistance = ((ChunkMapAccessor)player.serverLevel().getChunkSource().chunkMap).getViewDistance();

            for (int r = viewDistance; r <= distance-2 ; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) == r) {

                            int cx = centerX + x;
                            int cz = centerZ + z;

                            if (!sendChunks.contains(pack(cx, cz))) {
                                if(sendChunk(player, cx, cz)){
                                    sentThisTick++;
                                }
                                sendChunks.add(pack(cx, cz));

                            }

                            if(sentThisTick > chunksPerTick)return;
                        }
                    }
                }
            }
        }
    }

    public static boolean inRange(int centerX, int centerZ,long pos, int distance) {
        return Math.abs(unpackX(pos) - centerX) <= distance && Math.abs(unpackZ(pos) - centerZ) <= distance;
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

}
