package mod.mh48.rageload.client.worldgen.fakedata;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.compat.lithostitched.LithostitchedCompatHandler;
import mod.mh48.rageload.mixin.gen.fakedata.ChunkMapAccessor;
import mod.mh48.rageload.mixin.gen.fakedata.MinecraftServerAccessor;
import mod.mh48.rageload.platform.Services48;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.BooleanSupplier;

public class ClientGenServer extends MinecraftServer {

    private static final Services NO_SERVICES = new Services((MinecraftSessionService)null, ServicesKeySet.EMPTY, (GameProfileRepository)null, (GameProfileCache)null);

    private final Path rootPath;

    private final ChunkProgressListenerFactory chunkProgressListenerFactory;


    public ClientGenServer(Path rootPath, Thread pServerThread, LevelStorageSource.LevelStorageAccess pStorageSource, PackRepository pPackRepository, WorldStem pWorldStem, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(pServerThread, pStorageSource, pPackRepository, pWorldStem, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, chunkProgressListenerFactory);
        this.rootPath = rootPath;
        this.chunkProgressListenerFactory = chunkProgressListenerFactory;
    }

    @Override
    protected void runServer() {
        try {
            if (!this.initServer()) {
                throw new IllegalStateException("Failed to initialize server");
            }

            while(this.isRunning()) {
                long nextTickTime = Util.getMillis()+50L;
                ((MinecraftServerAccessor)this).setNextTickTime(nextTickTime);
                ((MinecraftServerAccessor)this).setDelayedTasksMaxNextTickTime(Util.getMillis()+50L);
                this.tickServer(() -> Util.getMillis() < nextTickTime);
                this.waitUntilNextTick();
                Thread.sleep(20);
            }
        } catch (Throwable throwable1) {
            RageLoad.LOGGER.error("Error in ClientGenServer: ",throwable1);
        } finally {
            try {
                this.stopServer();
            } catch (Throwable throwable) {

            }

        }

    }

    @Override
    protected boolean initServer() {
        this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, 1) {});
        if (Services48.PLATFORM.isModLoaded("lithostitched")) {
            LithostitchedCompatHandler.onServerAboutToStart(this);
        }
        this.loadLevel();
        ServerLevel serverlevel = this.overworld();
        serverlevel.setDefaultSpawnPos(new BlockPos(0,0,0), 0.0F);
        serverlevel.getChunkSource().removeRegionTicket(TicketType.START, new ChunkPos(0,0), 11, Unit.INSTANCE);
        serverlevel.setWeatherParameters(20000000, 20000000, false, false);
        //LOGGER.info("Started game test server");
        return true;
    }

    @Override
    public boolean saveAllChunks(boolean pSuppressLog, boolean pFlush, boolean pForced) {
        return true;//skip chunk saving
    }

    @Override
    public void stopServer() {
        super.stopServer();
        if (rootPath != null && Files.exists(rootPath)) {
            try {
                Files.walk(rootPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                RageLoad.LOGGER.warn("Error deleting tmp folder: ",e);
            }
        }
    }

    @Override
    protected void loadLevel() {
        ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
        this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
        ChunkProgressListener chunkprogresslistener = chunkProgressListenerFactory.create(1);
        this.createLevels(chunkprogresslistener);
        this.forceDifficulty();
        //this.prepareLevels(chunkprogresslistener); skip spawnchunk generation
        if (profiledduration != null) {
            profiledduration.finish();
        }

    }

    @Override
    public void tickServer(BooleanSupplier pHasTimeLeft) {
        //DO NOT TICK LOL
        for(ServerLevel level:this.getAllLevels()){
            level.noSave = true;
            ChunkMapAccessor mapKiller = ((ChunkMapAccessor) level.getChunkSource().chunkMap);//Let's nuke the shit out of the server (:<
            mapKiller.getUpdatingChunkMap().clear();
            mapKiller.getVisibleChunkMap().clear();
            mapKiller.getPendingUnloads().clear();

            level.getChunkSource().tick(() -> {
                return true;
            }, false);
        }
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 0;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport pReport) {
        return null;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return false;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile pProfile) {
        return false;
    }
}
