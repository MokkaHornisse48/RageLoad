package mod.mh48.rageload.duck.gen;

import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ProtoChunk;

public interface ProtoChunkDuck {
    void optiload$setWaitingGen(boolean waitingGen);
    boolean optiload$isWaitingGen();
    void optiload$setDependend(ProtoChunk chunk);

    void optiload$setLight(DataLayer[] lightSky,DataLayer[] lightBlock);
    DataLayer[] optiload$getLightSky();
    DataLayer[] optiload$getLightBlock();

    void optiload$setLoaded();
    boolean optiload$gotLoaded();
}
