package mod.mh48.rageload.client;

import io.netty.buffer.Unpooled;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.duck.client.ClientChunkCacheDuck;
import mod.mh48.rageload.net.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.zip.InflaterInputStream;

import static mod.mh48.rageload.Networking.*;

public class NetworkingClient {

    public static CompletableFuture<CompoundTag> request = null;//Should only ever exist once

    public static void init(){

        ClientNetworkManager.registerReceiver(GEN_DATA_PACKET, (buf, mc) -> {
            int totalSize = buf.readInt();
            byte[] chunkData = new byte[totalSize];
            buf.readBytes(chunkData);

            CompoundTag nbt;
            try {
                InputStream in = new InflaterInputStream(new ByteArrayInputStream(chunkData));
                nbt = NbtIo.read(new DataInputStream(in));
                request.complete(nbt);
                request = null;
            } catch (Exception e) {
                RageLoad.LOGGER.info("Failed to decode gendata\n", e);
                request.failedFuture(e);
                request = null;
            }


        });

        ClientNetworkManager.registerReceiver( CHUNK_DATA_PACKET, (buf, mc) -> {
            int x = buf.readInt();
            int z = buf.readInt();
            byte compression = buf.readByte();
            int datasize = buf.readInt();
            byte[] data = new byte[datasize];
            buf.readBytes(data);


            ((ClientChunkCacheDuck) mc.level.getChunkSource()).getWorldLoader().queue.add(new ClientWorldLoader.ClientChunkData(x,z,compression,data));
        });

        ClientNetworkManager.registerReceiver( STRUCTURE_DATA, ( buf, mc) -> {
            ResourceLocation rl = buf.readResourceLocation();
            CompoundTag data = buf.readAnySizeNbt();

            CompletableFuture<CompoundTag> future = structureRequest.remove(rl);
            if (future != null) {
                future.complete(data);
            }
        });

    }

    public static CompletableFuture<CompoundTag> requestGenData() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ClientNetworkManager.sendToServer(REQUEST_GEN_DATA_PACKET, buf);
        if(request!=null)request.completeExceptionally(new Exception("WTF are you doing this should never happen"));
        request = new CompletableFuture<>();
        return request;
    }

    public static void initWorldDataData(int distance) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(distance);
        ClientNetworkManager.sendToServer(INIT_WORLD_DATA_PACKET, buf);
    }


    public static ConcurrentHashMap<ResourceLocation,CompletableFuture<CompoundTag>> structureRequest = new ConcurrentHashMap<>();

    public static CompletableFuture<CompoundTag> requestStructureData(ResourceLocation resourceLocation)  {
        CompletableFuture<CompoundTag> existingFuture = structureRequest.get(resourceLocation);
        if (existingFuture != null && !existingFuture.isDone()) {
            return existingFuture;
        }

        CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        structureRequest.put(resourceLocation, future);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(resourceLocation);
        ClientNetworkManager.sendToServer(GET_STRUCTURE_DATA, buf);

        return future;
    }



    public static void clear(){
        if(request!=null)request.completeExceptionally(new Exception("Connection closed"));
        request = null;
    }
}
