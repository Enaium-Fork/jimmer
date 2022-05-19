package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface Field {

    ImmutableProp getProp();

    Filter<?,?> getFilter();

    int getBatchSize();

    int getLimit();

    RecursionStrategy<?> getRecursionStrategy();

    Fetcher<?> getChildFetcher();

    boolean isSimpleField();
}
