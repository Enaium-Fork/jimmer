package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.chain.CacheChain;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class ParameterizedCaches {

    private ParameterizedCaches() {}

    public static <K, V> Cache<K, V> create(ImmutableProp prop) {
        return create(prop, null, null);
    }

    public static <K, V> Cache<K, V> create(
            ImmutableProp prop,
            Consumer<Collection<String>> onDelete
    ) {
        return create(prop, onDelete, null);
    }
    
    public static <K, V> Cache<K, V> create(
            ImmutableProp prop,
            Consumer<Collection<String>> onDelete,
            Map<String, Map<String, byte[]>> valueMap
    ) {
        return new ChainCacheBuilder<K, V>()
                .add(new LevelOneBinder<>(prop))
                .add(new LevelTwoBinder<>(prop, onDelete, valueMap))
                .build();
    }

    private static <K, V> Map<K, V> read(
            Map<K, Map<Map<String, Object>, V>> valueMap,
            Collection<K> keys,
            Map<String, Object> parameterMap
    ) {
        Map<K, V> map = new HashMap<>();
        for (K key : keys) {
            Map<Map<String, Object>, V> subMap = valueMap.get(key);
            if (subMap != null) {
                V value = subMap.get(parameterMap);
                if (value != null || subMap.containsKey(parameterMap)) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static <K, V> Map<K, V> write(
            Map<K, Map<Map<String, Object>, V>> valueMap,
            Map<K, V> map,
            Map<String, Object> parameterMap
    ) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            Map<Map<String, Object>, V> subMap = valueMap.computeIfAbsent(entry.getKey(), it -> new HashMap<>());
            subMap.put(parameterMap, entry.getValue());
        }
        return map;
    }

    private static class LevelOneBinder<K, V> implements LoadingBinder.Parameterized<K, V> {

        private final ImmutableProp prop;
        
        private final Map<K, Map<Map<String, Object>, V>> valueMap = new HashMap<>();

        private CacheChain.Parameterized<K, V> chain;

        private LevelOneBinder(ImmutableProp prop) {
            this.prop = prop;
        }

        @Override
        public void initialize(@NotNull CacheChain.Parameterized<K, V> chain) {
            this.chain = chain;
        }

        @Override
        public @NotNull Map<K, V> getAll(
                @NotNull Collection<K> keys,
                @NotNull SortedMap<String, Object> parameterMap
        ) {
            Map<K, V> map = read(valueMap, keys, parameterMap);
            if (map.size() < keys.size()) {
                Set<K> missedKeys = new LinkedHashSet<>();
                for (K key : keys) {
                    if (!map.containsKey(key)) {
                        missedKeys.add(key);
                    }
                }
                Map<K, V> mapFromNext = chain.loadAll(keys, parameterMap);
                if (mapFromNext.size() < missedKeys.size()) {
                    for (K missedKey : missedKeys) {
                        if (!mapFromNext.containsKey(missedKey)) {
                            mapFromNext.put(missedKey, null);
                        }
                    }
                }
                write(valueMap, mapFromNext, parameterMap);
                map.putAll(mapFromNext);
            }
            return map;
        }

        @Override
        public @Nullable ImmutableType type() {
            return null;
        }

        @Override
        public @Nullable ImmutableProp prop() {
            return prop;
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
            valueMap.keySet().removeAll(keys);
        }
    }

    private static class LevelTwoBinder<K, V> extends AbstractRemoteHashBinder<K, V> {

        private final Map<String, Map<String, byte[]>> valueMap;

        private final Consumer<Collection<String>> onDelete;

        LevelTwoBinder(ImmutableProp prop, Consumer<Collection<String>> onDelete, Map<String, Map<String, byte[]>> valueMap) {
            super(null, prop, null, null, null, Duration.ofSeconds(10), 0);
            this.valueMap = valueMap != null ? valueMap : new HashMap<>();
            this.onDelete = onDelete;
        }

        @Override
        protected void deleteAllSerializedKeys(List<String> serializedKeys) {
            valueMap.keySet().removeAll(serializedKeys);
            if (onDelete != null) {
                onDelete.accept(serializedKeys);
            }
            valueMap.keySet().removeAll(serializedKeys);
        }

        @Override
        protected List<byte[]> read(Collection<String> keys, String hashKey) {
            List<byte[]> arr = new ArrayList<>();
            for (String key : keys) {
                Map<String, byte[]> subMap = valueMap.get(key);
                if (subMap != null) {
                    arr.add(subMap.get(hashKey));
                } else {
                    arr.add(null);
                }
            }
            return arr;
        }

        @Override
        protected void write(Map<String, byte[]> map, String hashKey) {
            for (Map.Entry<String, byte[]> e : map.entrySet()) {
                Map<String, byte[]> subMap = valueMap.computeIfAbsent(e.getKey(), it -> new HashMap<>());
                subMap.put(hashKey, e.getValue());
            }
        }

        @Override
        protected boolean matched(@Nullable Object reason) {
            return true;
        }
    }
}
