package mod.mh48.rageload.duck.gen;

import mod.mh48.rageload.client.worldgen.SaveProtoChunk;

public interface LevelChunkDuck {
    boolean wasGeneratedByClient();
    void setGeneratedByClient(boolean gen);

    void setSave(SaveProtoChunk save);
    SaveProtoChunk getSave();
}
