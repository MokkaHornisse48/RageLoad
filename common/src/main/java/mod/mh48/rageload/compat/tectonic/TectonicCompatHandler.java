package mod.mh48.rageload.compat.tectonic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.config.state.object.NoiseState;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class TectonicCompatHandler {

    public static void init() {
        ConfigConstant.DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(df -> "#48#"+df.value())
        ).apply(instance, TectonicCompatHandler::createConfigConstant));
        ConfigConstant.CODEC_HOLDER = KeyDispatchDataCodec.of(ConfigConstant.DATA_CODEC);

        ConfigNoise.DATA_CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
                Codec.STRING.fieldOf("key").forGetter((df) -> "#48#"+df.scale()+","+df.multiplier()+","+df.offset()),
                DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(ConfigNoise::noise),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(ConfigNoise::shiftX),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(ConfigNoise::shiftZ)
        ).apply(instance, TectonicCompatHandler::createConfigNoise));

        ConfigNoise.CODEC_HOLDER = KeyDispatchDataCodec.of(ConfigNoise.DATA_CODEC);
    }

    public static ConfigConstant createConfigConstant(String key) {
        ConfigConstant k;
        if (key.startsWith("#48#")) {
            k = new ConfigConstant(Double.parseDouble(key.substring(4)));
        } else {
            k = new ConfigConstant(ConfigHandler.getState().getValue(key));
        }
        return k;
    }


    public static ConfigNoise createConfigNoise(String key, DensityFunction.NoiseHolder noise, DensityFunction shiftX, DensityFunction shiftZ) {
        ConfigNoise k;
        if (key.startsWith("#48#")) {
            String[] vs = key.substring(4).split(",");
            NoiseState state = new NoiseState(Double.parseDouble(vs[0]),Double.parseDouble(vs[1]),Double.parseDouble(vs[2]));
            k = new ConfigNoise(noise, shiftX, shiftZ, state.scale, state.multiplier, state.offset);
        } else {
            k = ConfigNoise.create(key, noise, shiftX, shiftZ);
        }
        return k;
    }

    public static Tag getConfig(DynamicOps<Tag> ops) {
        return ConfigState.CODEC.encodeStart(ops,ConfigHandler.getState()).getOrThrow(false, error -> {
            System.err.println("Serialization failed: " + error);
        });
    }

    public static void loadConfig(DynamicOps<Tag> ops, Tag config) {
        ConfigState cs = ConfigState.CODEC.parse(ops, config)
                .getOrThrow(false, error -> {
                    System.err.println("Deserialization failed: " + error);
                });
        ConfigHandler.setState(cs);
    }
}
