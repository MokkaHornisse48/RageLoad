package mod.mh48.rageload.mixin.gen;


import mod.mh48.rageload.RageLoad;
import mod.mh48.rageload.duck.gen.ProtoChunkDuck;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin implements ProtoChunkDuck {

    @Shadow
    public abstract ChunkStatus getStatus();

    @Unique
    public AtomicBoolean optiload$waitingGen = new AtomicBoolean(false);

    @Unique
    public ArrayList<ProtoChunkDuck> optiload$dependent = new ArrayList<>();

    @Unique
    private DataLayer[] atrioffload$lightSky;

    @Unique
    public DataLayer[] atrioffload$lightBlock;


    @Unique
    public boolean optiload$loaded = false;

    @Unique
    @Override
    public void optiload$setWaitingGen(boolean waitingGen) {
        if(this.optiload$waitingGen.get()==waitingGen) RageLoad.LOGGER.error("DOUBLE WAITING_GEN DETECTED");
        this.optiload$waitingGen.set(waitingGen);
    }

    @Unique
    @Override
    public boolean optiload$isWaitingGen() {
        if(!optiload$dependent.isEmpty()) {
            for(ProtoChunkDuck dep:optiload$dependent){
                if(dep.optiload$isWaitingGen()){
                    return true;
                }
            }
            optiload$dependent.clear();
        }
        return optiload$waitingGen.get();
    }

    @Unique
    @Override
    public void optiload$setDependend(ProtoChunk chunk) {
        if(chunk.equals(this))return;
        if(chunk.getStatus().isOrAfter(this.getStatus()))return;
        optiload$dependent.add((ProtoChunkDuck) chunk);
    }

    @Unique
    @Override
    public void optiload$setLight(DataLayer[] lightSky, DataLayer[] lightBlock) {
        this.atrioffload$lightSky = lightSky;
        this.atrioffload$lightBlock = lightBlock;
    }

    @Unique
    @Override
    public DataLayer[] optiload$getLightSky() {
        return this.atrioffload$lightSky;
    }

    @Unique
    @Override
    public DataLayer[] optiload$getLightBlock() {
        return this.atrioffload$lightBlock;
    }

    @Unique
    @Override
    public void optiload$setLoaded(){
        optiload$loaded = true;
    }

    @Unique
    @Override
    public boolean optiload$gotLoaded(){
        return this.optiload$loaded;
    }

}
