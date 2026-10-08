package mod.mh48.rageload.client.worldgen;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.client.worldgen.faster.*;
import mod.mh48.rageload.compat.biomeswevegone.BiomeswevegoneCompatHandler;
import mod.mh48.rageload.duck.debug.ProtoChunkStatsDuck;
import mod.mh48.rageload.duck.gen.ProtoChunkDuck;
import mod.mh48.rageload.platform.Services48;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

//Warning this code can make you go insane
public class ClientChunkGenerator {

    public final MinecraftServer server;
    public final CompoundTag debugData;

    public ClientChunkGenerator(MinecraftServer server, CompoundTag debugData) {
        this.server = server;
        this.debugData = debugData;
    }


    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public void generateChunk(ResourceKey<Level> dim, ChunkPos pos, Function<ChunkPos, ProtoChunk> chunkGetter, BiConsumer<ProtoChunk,ServerLevel> chunkAdder){
        if(server==null||server.getLevel(dim)==null)return;
        ServerLevel level = server.getLevel(dim);
        if(generateToStatus(level, ChunkStatus.FULL,pos,chunkGetter)) {
            ProtoChunk chunk = chunkGetter.apply(pos);
            chunkAdder.accept(chunk,level);
        }
    }


    public boolean generateToStatus(ServerLevel level, ChunkStatus status, ChunkPos pos, Function<ChunkPos,ProtoChunk> chunkGetter){
        if(status==ChunkStatus.EMPTY)return true;
        RandomState rnd = level.getChunkSource().randomState();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        ProtoChunk chunk = chunkGetter.apply(pos);
        //if(chunkCache.get(chunkCache.size()/2)!=chunk)throw new IllegalStateException("chunkCache does not have GenCHunk in middle");

        if (chunk.getStatus()==ChunkStatus.EMPTY&&server.getWorldData().worldGenOptions().generateStructures()) {
            //WorldGenRegion region = getRegionWithState(level,ChunkStatus.EMPTY,pos,0,0,chunkGetter);
            //if(region==null)return false;

            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.STRUCTURE_STARTS);
            try {
                //gen.createStructures(server.registryAccess(), level.getChunkSource().getGeneratorState(), level.structureManager().forWorldGenRegion(region), chunk, server.getStructureManager());
            }catch (Exception e){
                RageLoad.LOGGER.error("Ignoring error making structure references: ",e);
            }
            chunk.setStatus(ChunkStatus.STRUCTURE_STARTS);
        }
        if(status==ChunkStatus.STRUCTURE_STARTS)return true;

        if(chunk.getStatus()==ChunkStatus.STRUCTURE_STARTS) {
            //WorldGenRegion  region = getRegionWithState(level,ChunkStatus.STRUCTURE_STARTS,pos,8,8,chunkGetter);//new WorldGenRegion(level, chunkCache, ChunkStatus.STRUCTURE_STARTS, 8);
            //if(region==null)return false;

            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.STRUCTURE_REFERENCES);
            //gen.createReferences(region, level.structureManager().forWorldGenRegion(region), chunk);
            chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
        }
        if(status==ChunkStatus.STRUCTURE_REFERENCES)return true;

        if(chunk instanceof SaveProtoChunk){
            chunk.setStatus(ChunkStatus.FULL);
            return true;
        }
        //We need to gen structure refs//starts for these chunks because those are not synced to client. The rest does not need to get generated because it's already there

        if(chunk.getStatus()==ChunkStatus.STRUCTURE_REFERENCES) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.STRUCTURE_REFERENCES,pos,-1,-1,chunkGetter);
            if(region==null)return false;
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.BIOMES);

            //((ProtoChunkDuck) chunk).optiload$setWaitingGen(true);
            //chunk.fillBiomesFromNoise();
            fillBiomesFromNoise2d(chunk,gen.getBiomeSource(), rnd.sampler(),(x,y)->200);//todo make configurable
            /*
            gen.createBiomes(Runnable::run, rnd, Blender.empty(), level.structureManager().forWorldGenRegion(region), chunk).thenApply((p_281193_) -> {
                return Either.left(p_281193_);
            }).whenComplete((bi, th) -> {
                chunk.setStatus(ChunkStatus.BIOMES);
                ((ProtoChunkDuck) chunk).optiload$setWaitingGen(false);
            });*/
            //return false;
            chunk.setStatus(ChunkStatus.BIOMES);
        }
        if(status==ChunkStatus.BIOMES)return true;

        if(chunk.getStatus()==ChunkStatus.BIOMES) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.BIOMES,pos,8,0,chunkGetter);
            if(region==null)return false;
            ((ProtoChunkDuck)chunk).optiload$setWaitingGen(true);
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.NOISE);
            //System.out.println(pos);
            CompletableFuture<Either<ChunkAccess, Object>> f;
            if(gen instanceof NoiseBasedChunkGenerator noiseGen){
                f = NoiseFill48.fillFromNoise(noiseGen, executor, Blender.empty(), rnd, level.structureManager().forWorldGenRegion(region), chunk).thenApply(Either::left);
                //f = gen.fillFromNoise(Util.backgroundExecutor(), Blender.empty(), rnd, level.structureManager().forWorldGenRegion(region), chunk).thenApply(Either::left);//todo make configurable
            }else{
                f = gen.fillFromNoise(Util.backgroundExecutor(), Blender.empty(), rnd, level.structureManager().forWorldGenRegion(region), chunk).thenApply(Either::left);
            }
            f.whenComplete((bi,th)->{
                chunk.setStatus(ChunkStatus.NOISE);
                ((ProtoChunkDuck)chunk).optiload$setWaitingGen(false);
            });
            return false;

        }
        if(status==ChunkStatus.NOISE)return true;

        if(chunk.getStatus()==ChunkStatus.NOISE) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.NOISE,pos,8,0,chunkGetter);
            if(region==null)return false;
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.SURFACE);

            ((ProtoChunkDuck)chunk).optiload$setWaitingGen(true);
            executor.execute(()->
                    {
                        //This is not thread safe but for some reason doesn't cause issues (:
                        //I mean it shouldn't really access other chunks.
                        gen.buildSurface(region, level.structureManager().forWorldGenRegion(region), rnd, chunk);
                        if (Services48.PLATFORM.isModLoaded("biomeswevegone")) {
                            BiomeswevegoneCompatHandler.onSurface(level,gen,chunk,region);
                        }
                        chunk.setStatus(ChunkStatus.SURFACE);


                        ((ProtoChunkDuck)chunk).optiload$setWaitingGen(false);
                    });
            return false;

        }
        if(status==ChunkStatus.SURFACE)return true;

        /* This could create a cool effect where you can see how chunks are being generated, but it would need some changes
        if(!(chunk instanceof SaveProtoChunk)){
            //chunk.setStatus(ChunkStatus.FULL);
            return true;
        }*/

        if(chunk.getStatus()==ChunkStatus.SURFACE) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.SURFACE,pos,8,8,chunkGetter);
            if(region==null)return false;
            //Blender.addAroundOldChunksCarvingMaskFilter(region, chunk);
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.CARVERS);
            gen.applyCarvers(region, level.getSeed(), rnd, level.getBiomeManager(), level.structureManager().forWorldGenRegion(region), chunk, GenerationStep.Carving.AIR);
            chunk.setStatus(ChunkStatus.CARVERS);
        }
        if(status==ChunkStatus.CARVERS)return true;


        if(chunk.getStatus()==ChunkStatus.CARVERS) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.CARVERS,pos,8,8,chunkGetter);
            if(region==null)return false;
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.FEATURES);

            try {
                gen.applyBiomeDecoration(region, chunk, level.structureManager().forWorldGenRegion(region));
            } catch (Exception e) {
                RageLoad.LOGGER.error("Ignoring error applying biome decoration: ", e);
            }
            chunk.setStatus(ChunkStatus.FEATURES);
        }
        if(status==ChunkStatus.FEATURES)return true;

        if(chunk.getStatus()==ChunkStatus.FEATURES) {
            WorldGenRegion region = getRegionWithState(level,ChunkStatus.INITIALIZE_LIGHT,pos,1,1,chunkGetter);
            if(region==null)return false;
            ((ProtoChunkDuck)chunk).optiload$setWaitingGen(true);
            ((ProtoChunkStatsDuck)chunk).atrioffload$startStatus(ChunkStatus.LIGHT);
            this.calculateLight(chunk,region);
            return false;
        }

        chunk.setStatus(status);//ensure
        /*


        engine.initializeLight(chunk, true).thenApply(Either::left).join();
        engine.lightChunk(chunk, true).thenApply(Either::left).join();
        engine.tryScheduleUpdate();*/

        /*
        ;*/
        return true;
    }

    public WorldGenRegion getRegionWithState(ServerLevel level, ChunkStatus status,ChunkPos center,int range,int state_range,Function<ChunkPos,ProtoChunk> chunkGetter){
        List<ChunkAccess> list = Lists.newArrayList();
        boolean waiting = false;
        for(int zi = center.z - range; zi <= center.z + range; ++zi) {
            for(int xi = center.x - range; xi <= center.x + range; ++xi) {
                ProtoChunk chunk = chunkGetter.apply(new ChunkPos(xi,zi));
                list.add(chunk);

                if(((ProtoChunkDuck)chunk).optiload$isWaitingGen()){
                    ((ProtoChunkDuck)chunkGetter.apply(center)).optiload$setDependend(chunk);
                    waiting = true;
                    continue;
                }

                chunk.setLightEngine(level.getLightEngine());
                int dis = center.getChessboardDistance(new ChunkPos(xi, zi));
                ChunkStatus gstatus = ChunkStatus.getStatusAroundFullChunk( ChunkStatus.getDistance(status) + dis + 1);

                if(status==ChunkStatus.CARVERS&&dis<2){//Maybe fix feature but does not realyy work. Idk tf I am doing anymore.
                    gstatus=ChunkStatus.CARVERS;
                }

                if(status==ChunkStatus.NOISE&&dis<3){//Optimization that alows further biome caching. Should improve terrablender
                    gstatus=ChunkStatus.BIOMES;
                }

                if(status==ChunkStatus.INITIALIZE_LIGHT){
                    gstatus=ChunkStatus.FEATURES;
                }

                if(!generateToStatus(level,gstatus, new ChunkPos(xi, zi), chunkGetter)){
                    ((ProtoChunkDuck)chunkGetter.apply(center)).optiload$setDependend(chunk);
                    waiting = true;
                }

                if(status==ChunkStatus.CARVERS&&dis<2) {
                    //Heightmap.primeHeightmaps(chunkGetter.apply(new ChunkPos(xi, zi)), EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
                }

            }
        }
        if(waiting)return null;
        if(range<0)list.add(chunkGetter.apply(center));
        return new WorldGenRegionNoBlending(level, list, ChunkStatus.STRUCTURE_REFERENCES, state_range);
    }

    public interface SurfaceLevelGetter{
        int getSurfaceLevel(int x, int z);
    }

    public static void fillBiomesFromNoise2d(ChunkAccess chunk, BiomeResolver biomeResolver, Climate.Sampler sampler,SurfaceLevelGetter sg) {

        ChunkPos chunkPos = chunk.getPos();
        int minX = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int minZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        LevelHeightAccessor levelHeightAccessor = chunk.getHeightAccessorForGeneration();

        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();

        Holder<Biome>[] biomes = new Holder[4 * 4];
        int minY = levelHeightAccessor.getMinBuildHeight();

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                int blockX = minBlockX + x * 4;
                int blockZ = minBlockZ + z * 4;
                int surfaceBlockY = sg.getSurfaceLevel(blockX, blockZ);
                if (surfaceBlockY == Integer.MAX_VALUE) surfaceBlockY = minY;
                int quartY = QuartPos.fromBlock(surfaceBlockY + 3);  // +3 Blocks über Surface
                biomes[x + z * 4] = biomeResolver.getNoiseBiome(
                        minX + x, quartY, minZ + z, sampler
                );
            }
        }

        for (int k = levelHeightAccessor.getMinSection(); k < levelHeightAccessor.getMaxSection(); k++) {
            LevelChunkSection levelChunkSection = chunk.getSection(chunk.getSectionIndexFromSectionY(k));
            int l = QuartPos.fromSection(k);
            levelChunkSection.fillBiomesFromNoise((xc,yc,zc, s)-> biomes[xc+zc*4], sampler, 0, l, 0);
        }
    }



    public class WorldGenRegionNoBlending extends WorldGenRegion{

        public WorldGenRegionNoBlending(ServerLevel pLevel, List<ChunkAccess> pCache, ChunkStatus pGeneratingStatus, int pWriteRadiusCutoff) {
            super(pLevel, pCache, pGeneratingStatus, pWriteRadiusCutoff);
        }

        public boolean isOldChunkAround(ChunkPos pPos, int pRadius) {
            return false;
        }

        @Override
        public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
            RageLoad.LOGGER.error("Uncached biome access distance:"+(this.getCenter().x-QuartPos.toSection(i))+","+(this.getCenter().z-QuartPos.toSection(k))+","+this.getChunk(QuartPos.toSection(i),QuartPos.toSection(k)).getStatus());
            return super.getUncachedNoiseBiome(i, j, k);
        }

        public int getChessboardDistance(ChunkPos chunkPos,int x,int z) {
            return Math.max(Math.abs(x - chunkPos.x), Math.abs(z - chunkPos.z));
        }
    }

    public void unload(){
        server.halt(true);
    }

    
    public void calculateLight(ProtoChunk chunk,WorldGenRegion region){
        ChunkPos center = region.getCenter();
        LevelLightEngine levelLightEngine = region.getLightEngine();
        AdvancedLightRegion lightData = new AdvancedLightRegion(region);
        for (int zi = center.z-1; zi <= center.z+1; ++zi) {
            for (int xi = center.x-1; xi <= center.x+1; ++xi) {
                lightData.initSkyLight(xi,zi);
            }
        }
        lightData.initQueuesFrom3x3();

        executor.execute(() ->{
            lightData.finalizeLight();
            ((ProtoChunkDuck)chunk).optiload$setLight(lightData.skyData[4],lightData.blockData[4]);
            chunk.setStatus(ChunkStatus.LIGHT);
            ((ProtoChunkDuck)chunk).optiload$setWaitingGen(false);
        });
    }

}
