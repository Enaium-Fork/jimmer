package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiConsumer;

class FetcherContext {

    private static final ThreadLocal<FetcherContext> FETCHER_CONTEXT_LOCAL = new ThreadLocal<>();

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final FetchingCache cache = new FetchingCache();

    private final Map<FetchedField, FetcherTask> taskMap = new LinkedHashMap<>();

    public static void using(
            JSqlClientImplementor sqlClient,
            Connection con,
            BiConsumer<FetcherContext, Boolean> block
    ) {
        FetcherContext ctx = FETCHER_CONTEXT_LOCAL.get();
        if (ctx != null) {
            block.accept(ctx, false);
        } else {
            ctx = new FetcherContext(sqlClient, con);
            FETCHER_CONTEXT_LOCAL.set(ctx);
            try {
                block.accept(ctx, true);
            } finally {
                FETCHER_CONTEXT_LOCAL.remove();
            }
        }
    }

    private FetcherContext(JSqlClientImplementor sqlClient, Connection con) {
        this.sqlClient = sqlClient;
        this.con = con;
    }

    public void addAll(FetchPath path, Fetcher<?> fetcher, Collection<@Nullable DraftSpi> drafts) {
        for (DraftSpi draft : drafts) {
            if (draft != null) {
                add(path, fetcher, draft);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void add(FetchPath path, Fetcher<?> fetcher, DraftSpi draft) {
        FetcherImplementor<?> fetcherImplementor = (FetcherImplementor<?>) fetcher;
        setVisibility(draft, (FetcherImplementor<?>) fetcher);
        for (Field field : fetcherImplementor.__unresolvedFieldMap().values()) {
            if (!field.isSimpleField() ||
                    (!field.isRawId() &&
                    sqlClient.getFilters().getFilter(field.getProp().getTargetType()) != null)
            ) {
                RecursionStrategy<?> recursionStrategy = field.getRecursionStrategy();
                if (recursionStrategy != null &&
                        !((RecursionStrategy<Object>) recursionStrategy).isRecursive(
                                new RecursionStrategy.Args<>(draft, 0)
                        )
                ) {
                    return;
                }
                FetcherTask task = taskMap.computeIfAbsent(new FetchedField(path, field), it ->
                        new FetcherTask(
                                cache,
                                sqlClient,
                                con,
                                path,
                                field
                        )
                );
                task.add(draft);
            }
        }
    }

    public void execute() {
        while (!taskMap.isEmpty()) {
            Iterator<Map.Entry<FetchedField, FetcherTask>> itr = taskMap.entrySet().iterator();
            Map.Entry<FetchedField, FetcherTask> e = itr.next();
            if (e.getValue().execute()) {
                taskMap.remove(e.getKey());
            }
        }
    }

    static void setVisibility(DraftSpi draft, FetcherImplementor<?> fetcher) {
        for (PropId shownPropId : fetcher.__shownPropIds()) {
            draft.__show(shownPropId, true);
        }
        for (PropId hiddenPropId : fetcher.__hiddenPropIds()) {
            draft.__show(hiddenPropId, false);
        }
        for (Field field : fetcher.getFieldMap().values()) {
            FetcherImplementor<?> childFetcher = (FetcherImplementor<?>) field.getChildFetcher(true);
            ImmutableProp prop = field.getProp();
            if (childFetcher != null && prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                PropId propId = prop.getId();
                if (draft.__isLoaded(propId)) {
                    DraftSpi childDraft = (DraftSpi) draft.__get(propId);
                    if (childDraft != null) {
                        setVisibility(childDraft, childFetcher);
                    }
                }
            }
        }
    }

    private static class FetchedField {

        final FetchPath path;

        final Field field;

        private FetchedField(FetchPath path, Field field) {
            this.path = path;
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FetchedField that = (FetchedField) o;

            if (!Objects.equals(path, that.path)) return false;
            return field.getProp().equals(that.field.getProp());
        }

        @Override
        public int hashCode() {
            int result = path != null ? path.hashCode() : 0;
            result = 31 * result + field.getProp().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "path='" + path + '\'' +
                    ", field=" + field +
                    '}';
        }
    }
}