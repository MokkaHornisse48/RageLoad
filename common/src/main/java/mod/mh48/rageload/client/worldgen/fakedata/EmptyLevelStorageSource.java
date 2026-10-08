package mod.mh48.rageload.client.worldgen.fakedata;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.PathAllowList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class EmptyLevelStorageSource extends LevelStorageSource {

    public final DataFixer fixerUpper;
    public final Path empty;

    public EmptyLevelStorageSource(Path empty,DataFixer pFixerUpper) {
        super(empty, empty, new DirectoryValidator(new PathAllowList(List.of())), pFixerUpper);
        this.fixerUpper = pFixerUpper;
        this.empty = empty;
    }

    public String getName() {
        return "Anvil";
    }

    public LevelCandidates findLevelCandidates() throws LevelStorageException {
        return null;
    }

    public CompletableFuture<List<LevelSummary>> loadLevelSummaries(LevelCandidates pCandidates) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    private int getStorageVersion() {
        return 19133;
    }

    <T> T readLevelData(LevelDirectory pLevelDirectory, BiFunction<Path, DataFixer, T> pLevelDatReader) {
        return null;
    }

    private static WorldDataConfiguration getDataConfiguration(Path pDataFile, DataFixer pFixer) {
        return null;
    }

    static BiFunction<Path, DataFixer, Pair<WorldData, WorldDimensions.Complete>> getLevelData(DynamicOps<Tag> pOps, WorldDataConfiguration pDataConfiguration, Registry<LevelStem> pLevelStemRegistry, Lifecycle pLifecycle) {
        return (p_265020_, p_265021_) -> {return null;};
    }

    BiFunction<Path, DataFixer, LevelSummary> levelSummaryReader(LevelDirectory pLevelDirectory, boolean pLocked) {
        return (p_289916_, p_289917_) -> null;
    }


    public boolean isNewLevelIdAcceptable(String pSaveName) {
        return true;
    }

    /**
     * Return whether the given world can be loaded.
     */
    public boolean levelExists(String pSaveName) {
        return true;
    }

    private Path getLevelPath(String pSaveName) {
        return empty;
    }

    public Path getBaseDir() {
        return empty;
    }

    /**
     * Gets the folder where backups are stored
     */
    public Path getBackupPath() {
        return empty;
    }

    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String pSaveName) throws IOException, ContentValidationException {
        return new LevelStorageAccess(pSaveName);
    }

    public LevelStorageSource.LevelStorageAccess createAccess(String pSaveName) throws IOException {
        Path path = this.getLevelPath(pSaveName);
        return new LevelStorageAccess(pSaveName);
    }

    public DirectoryValidator getWorldDirValidator() {
        return super.getWorldDirValidator();
    }

    public class LevelStorageAccess extends LevelStorageSource.LevelStorageAccess {

        public LevelStorageAccess(String pLevelId) throws IOException {
            super(pLevelId,empty);
        }

        public String getLevelId() {
            return super.getLevelId();
        }

        public Path getLevelPath(LevelResource pFolderName) {
            return empty;
        }

        public Path getDimensionPath(ResourceKey<Level> pDimensionPath) {
            return empty;
        }

        private void checkLock() {
        }

        public PlayerDataStorage createPlayerStorage() {
            return null;
        }

        public LevelSummary getSummary() {
            return null;
        }

        public Pair<WorldData, WorldDimensions.Complete> getDataTag(DynamicOps<Tag> pOps, WorldDataConfiguration pDataConfiguration, Registry<LevelStem> pLevelStemRegistry, Lifecycle pLifecycle) {
            return null;
        }

        public void readAdditionalLevelSaveData() {
        }

        public WorldDataConfiguration getDataConfiguration() {
            this.checkLock();
            return EmptyLevelStorageSource.this.readLevelData(null, EmptyLevelStorageSource::getDataConfiguration);
        }

        public void saveDataTag(RegistryAccess pRegistries, WorldData pServerConfiguration) {
        }

        public void saveDataTag(RegistryAccess pRegistries, WorldData pServerConfiguration, CompoundTag pHostPlayerNBT) {
        }

        public Optional<Path> getIconFile() {
            return Optional.empty();
        }

        public Path getWorldDir() {
            return empty;
        }

        public void deleteLevel() throws IOException {
        }

        public void renameLevel(String pSaveName) throws IOException {
        }

        public long makeWorldBackup() throws IOException {
            return 1;
        }

        public void close() throws IOException {
        }
    }
}
