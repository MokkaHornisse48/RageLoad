package mod.mh48.rageload.client.worldgen.faster;

import net.minecraft.world.level.levelgen.DensityFunction;

public class MutableSinglePointContext implements DensityFunction.FunctionContext {
    public int blockX;
    public int blockY;
    public int blockZ;

    @Override
    public int blockX() {
        return blockX;
    }

    @Override
    public int blockY() {
        return blockY;
    }

    @Override
    public int blockZ() {
        return blockZ;
    }

    public void set(int blockX, int blockY, int blockZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }
}
