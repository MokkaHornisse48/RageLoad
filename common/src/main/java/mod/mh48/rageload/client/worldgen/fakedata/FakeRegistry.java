package mod.mh48.rageload.client.worldgen.fakedata;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.MapMaker;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import mod.mh48.rageload.mixin.gen.fakedata.HolderSetNamedAccessor;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FakeRegistry<T> implements Registry<T>, HolderOwner<T> {
    /// This thing is so stupid and causes so many problems. But I am in to deep, and it works for now.

    private final HashMap<ResourceLocation, T> entries = new HashMap();
    private final HashMap<T, ResourceLocation> entriesInversed = new HashMap();
    private final BiMap<ResourceLocation, Integer> loc2Id = HashBiMap.create();

    private volatile Map<ResourceLocation, HolderSet.Named<T>> tags = new HashMap<>();

    private final ResourceKey<? extends Registry<T>> key;

    public FakeRegistry(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
    }

    int id = 10000;

    public void add(ResourceLocation pName, T el) {
        entries.put(pName, el);
        entriesInversed.put(el, pName);
        loc2Id.put(pName, id);
        id++;
    }

    public void add(ResourceLocation pName, T el, int id) {
        entries.put(pName, el);
        entriesInversed.put(el, pName);
        loc2Id.put(pName, id);
    }

    public void remove(ResourceLocation pName) {
        loc2Id.remove(pName);
        entriesInversed.remove(this.get(pName), pName);
        entries.remove(pName);
    }

    public RegistryAccess.RegistryEntry<?>[] getEntries() {
        return entries.values().stream().map((e) -> {
            assert e instanceof Registry<?>;
            var r = (Registry<?>) e;
            return entry(r);
        }).toList().toArray(new RegistryAccess.RegistryEntry<?>[0]);
    }

    public static <E> RegistryAccess.RegistryEntry<E> entry(Registry<E> registry) {
        return new RegistryAccess.RegistryEntry<>(registry.key(), registry);
    }


    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return key;
    }

    @Override
    public @Nullable ResourceLocation getKey(T pValue) {
        return entriesInversed.get(pValue);
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T pValue) {
        ResourceLocation Vkey = this.getKey(pValue);
        if (Vkey == null) return Optional.empty();
        return Optional.of(ResourceKey.create(this.key(), Vkey));
    }

    @Override
    public int getId(@Nullable T pValue) {
        return loc2Id.get(this.getKey(pValue));
    }

    @Override
    public @Nullable T byId(int pId) {
        return this.get(loc2Id.inverse().get(pId));
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public @Nullable T get(@Nullable ResourceKey<T> pKey) {
        T r = entries.get(pKey.location());
        if (r instanceof LevelStem stem) {
            if (stem.type() instanceof Holder.Direct<DimensionType> direct) {
                Holder.Reference<DimensionType> ref = Holder.Reference.createStandAlone(null, ResourceKey.create(Registries.DIMENSION_TYPE, pKey.location()));
                ref.bindValue(direct.value());
                r = (T) new LevelStem(ref, stem.generator());
            }
        }
        return r;
    }

    @Override
    public @Nullable T get(@Nullable ResourceLocation pName) {
        return entries.get(pName);
    }

    @Override
    public Lifecycle lifecycle(T p_123012_) {
        return Lifecycle.stable();
    }

    @Override
    public Lifecycle registryLifecycle() {
        return Lifecycle.stable();
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return entries.keySet();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return entries.entrySet().stream()
                .map((e) ->
                        new AbstractMap.SimpleEntry<>(ResourceKey.create(this.key(), e.getKey()), e.getValue())
                ).collect(Collectors.toSet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return this.keySet().stream()
                .map((e) ->
                        ResourceKey.create(this.key(), e)
                ).collect(Collectors.toSet());
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource pRandom) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        // Da du valueToId nutzt, kannst du einen zufälligen Index würfeln
        int randomIndex = pRandom.nextInt(entries.size());

        // Wir brauchen das Objekt an diesem Index.
        // Falls du meine 'holderCache' Map von vorhin nutzt:
        T randomValue = entries.values().stream().skip(randomIndex).findFirst().orElse(null);

        if (randomValue != null) {
            ResourceLocation loc = entriesInversed.get(randomValue);
            return getHolder(ResourceKey.create(this.key, loc));
        }

        return Optional.empty();
    }

    @Override
    public boolean containsKey(ResourceLocation pName) {
        return this.get(pName) != null;
    }

    @Override
    public boolean containsKey(ResourceKey<T> pKey) {
        return this.get(pKey) != null;
    }

    @Override
    public Registry<T> freeze() {
        return this;
    }

    @Override
    public Holder.Reference<T> createIntrusiveHolder(T pValue) {
        return this.wrapAsHolder(pValue);
    }

    private static final ConcurrentMap<ResourceKey, Holder.Reference<?>> HOLDERS = new MapMaker().weakValues().makeMap();

    @Override
    public Optional<Holder.Reference<T>> getHolder(int pId) {
        return this.getHolder(ResourceKey.create(this.key(), Optional.ofNullable(loc2Id.inverse().get(pId)).orElse(new ResourceLocation("null:null"))));
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> pKey) {
        Holder.Reference<T> ref = (Holder.Reference<T>) HOLDERS.computeIfAbsent(pKey, (k) -> Holder.Reference.createStandAlone(this.holderOwner(), k));
        ref.bindValue(entries.get(pKey.location()));
        return Optional.ofNullable(ref);
    }

    @Override
    public Holder.Reference<T> wrapAsHolder(T pValue) {
        Optional<ResourceKey<T>> Vkey = this.getResourceKey(pValue);
        if (Vkey.isEmpty()) {
            Holder.Reference<T> ref = Holder.Reference.createStandAlone(this.holderOwner(), ResourceKey.create(this.key(), new ResourceLocation("null:null")));
            ref.bindValue(pValue);
            return ref;
        }
        return this.getHolder(Vkey.get()).get();
    }

    @Override
    public Stream<Holder.Reference<T>> holders() {
        return this.registryKeySet().stream().map((k) -> getHolder(k).get());
    }

    @Override
    public Optional<HolderSet.Named<T>> getTag(TagKey<T> pKey) {
        return Optional.ofNullable(tags.get(pKey.location()));
    }

    @Override
    public HolderSet.Named<T> getOrCreateTag(TagKey<T> pKey) {
        if (!tags.containsKey(pKey.location())) {
            tags.put(pKey.location(), HolderSetNamedAccessor.create(this, pKey));
        }
        return tags.get(pKey.location());

    }

    @Override
    public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
        return this.tags.entrySet().stream().map((p_211060_) -> {
            return Pair.of(TagKey.create(this.key(), p_211060_.getKey()), p_211060_.getValue());
        });
    }

    @Override
    public Stream<TagKey<T>> getTagNames() {
        return this.tags.keySet().stream().map((t) -> TagKey.create(this.key(), t));
    }

    @Override
    public void resetTags() {
        this.tags.values().forEach((tag) -> {
            tag.bind(List.of());
        });
    }

    @Override
    public void bindTags(Map<TagKey<T>, List<Holder<T>>> pTagMap) {
        throw new RuntimeException("bindTags not Implemented by FakeArray");
    }

    @Override
    public HolderOwner<T> holderOwner() {
        return this;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> pOwner) {
        return true;
    }

    @Override
    public HolderLookup.RegistryLookup<T> asLookup() {
        return new HolderLookup.RegistryLookup<T>() {
            public ResourceKey<? extends Registry<? extends T>> key() {
                return FakeRegistry.this.key;
            }

            public Lifecycle registryLifecycle() {
                return FakeRegistry.this.registryLifecycle();
            }

            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255624_) {
                return FakeRegistry.this.getHolder(p_255624_);
            }

            public Stream<Holder.Reference<T>> listElements() {
                return FakeRegistry.this.holders();
            }

            public Optional<HolderSet.Named<T>> get(TagKey<T> p_256277_) {
                return FakeRegistry.this.getTag(p_256277_);
            }

            public Stream<HolderSet.Named<T>> listTags() {
                return FakeRegistry.this.getTags().map(Pair::getSecond);
            }
        };
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return entries.values().iterator();
    }
}
