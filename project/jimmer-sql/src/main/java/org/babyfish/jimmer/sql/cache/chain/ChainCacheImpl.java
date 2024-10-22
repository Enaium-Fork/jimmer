package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

class ChainCacheImpl<K, V> implements Cache<K, V> {

    private static final ThreadLocal<CacheLoader<?, ?>> LOADER_LOCAL =
        new ThreadLocal<>();

    protected final ImmutableType type;

    protected final ImmutableProp prop;

    protected final Node<K, V> node;

    @SuppressWarnings("unchecked")
    public ChainCacheImpl(List<Binder<K>> binders) {
        if (binders.isEmpty()) {
            throw new IllegalArgumentException("binders cannot be empty");
        }
        ImmutableType cacheType = null;
        ImmutableProp cacheProp = null;
        Node<K, V> node = this.createTailNode();
        ListIterator<Binder<K>> itr = binders.listIterator(binders.size());
        while (itr.hasPrevious()) {
            Binder<K> binder = itr.previous();
            ImmutableType type = binder.type();
            ImmutableProp prop = binder.prop();
            if (cacheType == null) {
                cacheType = type;
                cacheProp = prop;
            } else {
                if (cacheType != type || cacheProp != prop) {
                    throw new IllegalArgumentException(
                            "Not all binders belong to same type/prop"
                    );
                }
            }
            node = createNode(binder, node);
        }
        this.type = cacheType;
        this.prop = cacheProp;
        this.node = node;
    }

    @Override
    public @NotNull ImmutableType type() {
        return type;
    }

    @Override
    public @Nullable ImmutableProp prop() {
        return prop;
    }

    @NotNull
    @Override
    public Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env) {
        return usingCacheLoading(env.getLoader(), () -> node.loadAll(keys));
    }

    @Override
    public void deleteAll(@NotNull Collection<K> keys, Object reason) {
        node.deleteAll(keys, reason);
    }

    @SuppressWarnings("unchecked")
    protected Node<K, V> createNode(Object binder, Node<K, V> next) {
        if (binder instanceof LoadingBinder<?, ?>) {
            return new LoadingNode<>((LoadingBinder<K, V>) binder, next);
        }
        return new SimpleNode<>((SimpleBinder<K, V>) binder, next);
    }

    protected TailNode<K, V> createTailNode() {
        return new TailNode<>();
    }

    protected interface Node<K, V> extends CacheChain<K, V> {
        void deleteAll(@NotNull Collection<K> keys, Object reason);
    }

    private static class LoadingNode<K, V> implements Node<K, V> {

        private final LoadingBinder<K, V> binder;

        private final Node<K, V> next;

        LoadingNode(LoadingBinder<K, V> binder, Node<K, V> next) {
            this.binder = binder;
            this.next = next;
            binder.initialize(next);
        }

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            return binder.getAll(keys);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
            next.deleteAll(keys, reason);
            binder.deleteAll(keys, reason);
        }
    }

    protected static class SimpleNode<K, V> implements Node<K, V> {

        protected final SimpleBinder<K, V> binder;

        protected final Node<K, V> next;

        protected SimpleNode(SimpleBinder<K, V> binder, Node<K, V> next) {
            this.binder = binder;
            this.next = next;
        }

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            Map<K, V> map = binder.getAll(keys);
            if (map.size() < keys.size()) {
                if (binder instanceof LockedBinder<?, ?>) {
                    LockedBinder<?, ?> lockedBinder = (LockedBinder<?, ?>) binder;
                    Set<K> missedKeys = missedKeys(keys, map);
                    try {
                        lockedBinder.locker().locking(
                                lockedBinder.unwrap(),
                                missedKeys,
                                lockedBinder.waitDuration(),
                                lockedBinder.leaseDuration(),
                                locked -> {
                                    loadAllForNext(missedKeys, map, locked);
                                }
                        );
                    } catch (ExecutionException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new ExecutionException(
                                "Failed to load missed data and update cache",
                                ex
                        );
                    }
                } else {
                    loadAllForNext(missedKeys(keys, map), map, true);
                }
            }
            return map;
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
            if (keys.isEmpty()) {
                return;
            }
            if (binder instanceof LockedBinder<?, ?>) {
                LockedBinder<?, ?> lockedBinder = (LockedBinder<?, ?>) binder;
                try {
                    lockedBinder.locker().locking(
                            lockedBinder.unwrap(),
                            keys instanceof Set<?> ?
                                    (Set<K>) keys :
                                    new LinkedHashSet<>(keys),
                            null,
                            lockedBinder.leaseDuration(),
                            locked -> {
                                next.deleteAll(keys, reason);
                                binder.deleteAll(keys, reason);
                            }
                    );
                } catch (ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "Failed to delete keys from cache",
                            ex
                    );
                }
            } else {
                next.deleteAll(keys, reason);
                binder.deleteAll(keys, reason);
            }
        }

        private static <K, V> Set<K> missedKeys(Collection<K> keys, Map<K, V> loadedMap) {
            Set<K> missedKeys = new LinkedHashSet<>();
            for (K key : keys) {
                if (!loadedMap.containsKey(key)) {
                    missedKeys.add(key);
                }
            }
            return missedKeys;
        }

        private void loadAllForNext(
                Collection<K> missedKeys,
                Map<K, V> loadedMap,
                boolean updateBinder
        ) {
            Map<K, V> mapFromNext = next.loadAll(missedKeys);
            if (mapFromNext.size() < missedKeys.size()) {
                mapFromNext = new HashMap<>(mapFromNext);
                if (updateBinder) {
                    for (K missedKey : missedKeys) {
                        if (!mapFromNext.containsKey(missedKey)) {
                            mapFromNext.put(missedKey, null);
                        }
                    }
                }
            }
            if (updateBinder) {
                binder.setAll(mapFromNext);
            }
            loadedMap.putAll(mapFromNext);
        }
    }

    protected static class TailNode<K, V> implements Node<K, V> {

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            CacheLoader<K, V> loader = currentCacheLoader();
            return loader.loadAll(keys);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
        }
    }

    protected static <R> R usingCacheLoading(
            CacheLoader<?, ?> loader,
            Supplier<R> block
    ) {
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        CacheLoader<?, ?> oldLoader = LOADER_LOCAL.get();
        LOADER_LOCAL.set(loader);
        try {
            return block.get();
        } finally {
            if (oldLoader != null) {
                LOADER_LOCAL.set(oldLoader);
            } else {
                LOADER_LOCAL.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <K, V> CacheLoader<K, V> currentCacheLoader() {
        CacheLoader<?, ?> loader = LOADER_LOCAL.get();
        if (loader == null) {
            throw new IllegalStateException(
                    "Cache binder can only be called by chain cache"
            );
        }
        return (CacheLoader<K, V>) loader;
    }
}
