package mod.mh48.rageload.mixin.gen.fakedata;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(HolderSet.Named.class)
public interface HolderSetNamedAccessor <T>{
    @Invoker("<init>")
    static <T> HolderSet.Named<T> create(HolderOwner<T> pOwner, TagKey<T> pKey) {
        throw new UnsupportedOperationException();
    }

    @Accessor("contents")
    List<Holder<T>> getContents();
}
