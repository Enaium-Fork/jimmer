package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.di.*;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetaStringResolver;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JSqlClient extends SubQueryProvider {

    static Builder newBuilder() {
        return new JSqlClientImpl.BuilderImpl();
    }

    <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table);

    MutableUpdate createUpdate(TableProxy<?> table);

    MutableDelete createDelete(TableProxy<?> table);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(
            AssociationTable<SE,ST, TE, TT> table
    );

    Entities getEntities();

    /**
     * This method is equivalent to `getTriggers(false)`
     * @return
     */
    Triggers getTriggers();

    /**
     * <ul>
     *     <li>
     *         If trigger type is 'BINLOG_ONLY'
     *         <ul>
     *             <li>If `transaction` is true, throws exception</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *     </li>
     *     <li>
     *         If trigger type is 'TRANSACTION_ONLY', returns transaction trigger
     *         no matter what the `transaction` is
     *     </li>
     *     <li>
     *         If trigger type is 'BOTH'
     *         <ul>
     *             <li>If `transaction` is true, return transaction trigger</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *         Note that the objects returned by different parameters are independent of each other.
     *     </li>
     * </ul>
     * @param transaction
     * @return Trigger
     */
    Triggers getTriggers(boolean transaction);

    Associations getAssociations(TypedProp.Association<?, ?> prop);

    Associations getAssociations(ImmutableProp immutableProp);

    Associations getAssociations(AssociationType associationType);

    Caches getCaches();

    Filters getFilters();

    BinLog getBinLog();

    @NewChain
    JSqlClient caches(Consumer<CacheDisableConfig> block);

    @NewChain
    JSqlClient filters(Consumer<FilterConfig> block);

    @NewChain
    JSqlClient disableSlaveConnectionManager();

    @NewChain
    JSqlClient executor(Executor executor);

    @Nullable
    default <T> T findById(Class<T> type, Object id) {
        return getEntities().findById(type, id);
    }

    @Nullable
    default <E> E findById(Fetcher<E> fetcher, Object id) {
        return getEntities().findById(fetcher, id);
    }

    @NotNull
    default <T> List<T> findByIds(Class<T> type, Collection<?> ids) {
        return getEntities().findByIds(type, ids);
    }

    @NotNull
    default <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids) {
        return getEntities().findByIds(fetcher, ids);
    }

    @NotNull
    default <K, V> Map<K, V> findMapByIds(Class<V> type, Collection<K> ids) {
        return getEntities().findMapByIds(type, ids);
    }

    @NotNull
    default <K, V> Map<K, V> findMapByIds(Fetcher<V> fetcher, Collection<K> ids) {
        return getEntities().findMapByIds(fetcher, ids);
    }

    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return getEntities().saveCommand(entity)
                .setMode(mode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(E entity, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(E entity) {
        return getEntities().saveCommand(entity).execute();
    }

    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveCommand(input.toEntity())
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return getEntities()
                .saveCommand(input.toEntity())
                .setMode(mode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(Input<E> input, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveCommand(input.toEntity())
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return getEntities().saveCommand(input.toEntity()).execute();
    }

    default <E> SimpleSaveResult<E> insert(E entity) {
        return save(entity, SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND);
    }

    default <E> SimpleSaveResult<E> insert(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.INSERT_ONLY, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> insertIfAbsent(E entity) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    default <E> SimpleSaveResult<E> insertIfAbsent(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> update(E entity) {
        return save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    default <E> SimpleSaveResult<E> update(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.UPDATE_ONLY, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> merge(E entity) {
        return save(entity, SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    default <E> SimpleSaveResult<E> merge(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.UPSERT, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> insert(Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND);
    }

    default <E> SimpleSaveResult<E> insert(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input.toEntity(), SaveMode.INSERT_ONLY, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> insertIfAbsent(Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    default <E> SimpleSaveResult<E> insertIfAbsent(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> update(Input<E> input) {
        return save(input.toEntity(), SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    default <E> SimpleSaveResult<E> update(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input.toEntity(), SaveMode.UPDATE_ONLY, associatedSaveMode);
    }

    default <E> SimpleSaveResult<E> merge(Input<E> input) {
        return save(input.toEntity(), SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    default <E> SimpleSaveResult<E> merge(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input.toEntity(), SaveMode.UPSERT, associatedSaveMode);
    }

    default <E> BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return getEntities()
                .saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode) {
        return getEntities()
                .saveEntitiesCommand(entities)
                .setMode(mode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveEntities(Iterable<E> entities) {
        return getEntities()
                .saveEntitiesCommand(entities)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(
            Iterable<Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return getEntities()
                .saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(Iterable<Input<E>> inputs, SaveMode mode) {
        return getEntities()
                .saveInputsCommand(inputs)
                .setMode(mode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(Iterable<Input<E>> inputs, AssociatedSaveMode associatedMode) {
        return getEntities()
                .saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    default <E> BatchSaveResult<E> saveInputs(Iterable<Input<E>> inputs) {
        return getEntities()
                .saveInputsCommand(inputs)
                .execute();
    }

    default DeleteResult deleteById(Class<?> type, Object id, DeleteMode mode) {
        return getEntities().delete(type, id, mode);
    }

    default DeleteResult deleteById(Class<?> type, Object id) {
        return getEntities().delete(type, id, DeleteMode.AUTO);
    }

    default DeleteResult deleteByIds(Class<?> type, Collection<?> ids, DeleteMode mode) {
        return getEntities().deleteAll(type, ids, mode);
    }

    default DeleteResult deleteByIds(Class<?> type, Collection<?> ids) {
        return getEntities().deleteAll(type, ids, DeleteMode.AUTO);
    }

    interface Builder {

        int DEFAULT_BATCH_SIZE = 128;

        int DEFAULT_LIST_BATCH_SIZE = 16;

        @OldChain
        Builder setConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setSlaveConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setDialect(Dialect dialect);

        @OldChain
        Builder setExecutor(Executor executor);

        /**
         * If this option is configured, when jimmer calls back
         * `org.babyfish.jimmer.sql.runtime.Executor.execute` before executing SQL,
         * it will check the stack trace information of the current thread.
         *
         * However, these stack traces have too much information, including
         * infrastructure call frames represented by jdk, jdbc driver, jimmer, and spring,
         * and the business-related information you care about will be submerged in the ocean of information.
         *
         * Through this configuration, you can specify multiple package or class prefixes, and jimmer will
         * judge whether there are some call frames in the stack trace whose class names start with some
         * of these prefixes. If the judgment is true, jimmer believes that the current callback is related
         * to your business, and the `ctx` parameter of `org.babyfish.jimmer.sql.runtime.Executor.execute`
         * will be passed as non-null.
         *
         * If the SQL logging configuration is enabled at the same time, when a SQL statement is caused by
         * the business you care about, the business call frame will be printed together with the SQL log.
         */
        @OldChain
        Builder setExecutorContextPrefixes(Collection<String> prefixes);

        @OldChain
        Builder setSqlFormatter(SqlFormatter formatter);

        @OldChain
        Builder setZoneId(@Nullable ZoneId zoneId);

        @OldChain
        Builder setUserIdGeneratorProvider(UserIdGeneratorProvider userIdGeneratorProvider);

        @OldChain
        Builder setLogicalDeletedValueGeneratorProvider(LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider);

        @OldChain
        Builder setTransientResolverProvider(TransientResolverProvider transientResolverProvider);

        @OldChain
        Builder setIdGenerator(IdGenerator idGenerator);

        @OldChain
        Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator);

        @OldChain
        Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setScalarProvider(TypedProp<?, ?> prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setScalarProvider(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setDefaultSerializedTypeObjectMapper(ObjectMapper mapper);

        @OldChain
        Builder setSerializedTypeObjectMapper(Class<?> type, ObjectMapper mapper);

        @OldChain
        Builder setSerializedPropObjectMapper(TypedProp<?, ?> prop, ObjectMapper mapper);

        @OldChain
        Builder setSerializedPropObjectMapper(ImmutableProp prop, ObjectMapper mapper);

        @OldChain
        Builder setDefaultJsonProviderCreator(Function<ImmutableProp, ScalarProvider<?, ?>> creator);

        @OldChain
        Builder setDefaultEnumStrategy(EnumType.Strategy strategy);

        @OldChain
        Builder setDatabaseNamingStrategy(DatabaseNamingStrategy strategy);

        @OldChain
        Builder setMetaStringResolver(MetaStringResolver resolver);

        @OldChain
        Builder setDefaultBatchSize(int size);

        @OldChain
        Builder setDefaultListBatchSize(int size);

        @OldChain
        Builder setInListPaddingEnabled(boolean enabled);

        @OldChain
        Builder setExpandedInListPaddingEnabled(boolean enabled);

        /**
         * For RDBMS, pagination is slow if `offset` is large, especially for MySQL.
         *
         * If `offset` >= $thisArgument
         *
         * <pre>{@code
         *  select t.* from Table t ... limit ? offset ?
         * }</pre>
         *
         * will be automatically changed to
         *
         * <pre>{@code
         *  select t.* from (
         *      select
         *          t.id as optimized_core_id_
         *      from Table t ... limit ? offset ?
         *  ) optimized_core_
         *  inner join Table as optimized_
         *      on optimized_.optimized_core_id_ = optimized_core_.optimized_core_id_
         * }</pre>
         *
         * @return An integer which is greater than 0
         */
        @OldChain
        Builder setOffsetOptimizingThreshold(int threshold);

        /**
         * Set deault lock mode of save command
         * @param lockMode
         */
        @OldChain
        Builder setDefaultLockMode(LockMode lockMode);

        @OldChain
        Builder setMaxCommandJoinCount(int maxMutationSubQueryDepth);

        /**
         * Under normal circumstances, users do not need to set the entity manager.
         *
         * This configuration is for compatibility with version 0.7.47 and earlier.
         */
        @OldChain
        Builder setEntityManager(EntityManager entityManager);

        @OldChain
        Builder setCaches(Consumer<CacheConfig> block);

        @OldChain
        Builder setCacheFactory(CacheFactory cacheFactory);

        @OldChain
        Builder setCacheOperator(CacheOperator cacheOperator);

        @OldChain
        Builder addCacheAbandonedCallback(CacheAbandonedCallback callback);

        @OldChain
        Builder addCacheAbandonedCallbacks(Collection<? extends CacheAbandonedCallback> callbacks);

        @OldChain
        Builder setTriggerType(TriggerType triggerType);

        @OldChain
        Builder setLogicalDeletedBehavior(LogicalDeletedBehavior behavior);

        @OldChain
        Builder addFilters(Filter<?>... filters);

        @OldChain
        Builder addFilters(Collection<? extends Filter<?>> filters);

        @OldChain
        Builder addDisabledFilters(Filter<?>... filters);

        @OldChain
        Builder addDisabledFilters(Collection<? extends Filter<?>> filters);

        @OldChain
        Builder setDefaultDissociateActionCheckable(boolean checkable);

        @OldChain
        Builder setIdOnlyTargetCheckingLevel(IdOnlyTargetCheckingLevel checkingLevel);

        @OldChain
        Builder addDraftPreProcessor(DraftPreProcessor<?> processor);

        @OldChain
        Builder addDraftPreProcessors(DraftPreProcessor<?>... processors);

        @OldChain
        Builder addDraftPreProcessors(Collection<DraftPreProcessor<?>> processors);

        @OldChain
        Builder addDraftInterceptor(DraftInterceptor<?, ?> interceptor);

        @OldChain
        Builder addDraftInterceptors(DraftInterceptor<?, ?>... interceptors);

        @OldChain
        Builder addDraftInterceptors(Collection<? extends DraftInterceptor<?, ?>> interceptors);

        Builder setDefaultBinLogObjectMapper(ObjectMapper mapper);

        @OldChain
        Builder setBinLogPropReader(ImmutableProp prop, BinLogPropReader reader);

        @OldChain
        Builder setBinLogPropReader(TypedProp.Scalar<?, ?> prop, BinLogPropReader reader);

        @OldChain
        Builder setBinLogPropReader(Class<?> propType, BinLogPropReader reader);

        @OldChain
        Builder setForeignKeyEnabledByDefault(boolean enabled);

        @OldChain
        Builder setTargetTransferable(boolean targetTransferable);

        @OldChain
        Builder addExceptionTranslator(ExceptionTranslator<?> translator);

        @OldChain
        Builder addExceptionTranslators(Collection<ExceptionTranslator<?>> translators);

        @OldChain
        Builder addCustomizers(Customizer ... customizers);

        @OldChain
        Builder addCustomizers(Collection<? extends Customizer> customizers);

        @OldChain
        Builder addInitializers(Initializer ... initializers);

        @OldChain
        Builder addInitializers(Collection<? extends Initializer> initializers);

        @OldChain
        Builder setDatabaseValidationMode(DatabaseValidationMode mode);

        @OldChain
        Builder setDatabaseValidationCatalog(String catalog);

        @OldChain
        Builder setDatabaseValidationSchema(String schema);

        @OldChain
        Builder setAopProxyProvider(AopProxyProvider provider);

        @OldChain
        Builder setMicroServiceName(String microServiceName);

        @OldChain
        Builder setMicroServiceExchange(MicroServiceExchange exchange);

        @OldChain
        Builder setInitializationType(InitializationType type);

        JSqlClient build();
    }
}
