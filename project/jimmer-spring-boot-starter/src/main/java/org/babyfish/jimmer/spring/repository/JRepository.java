package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.impl.util.CollectionUtils;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.spring.repository.support.Utils;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Data Repository
 * @param <E> The entity type
 * @param <ID> The entity id type
 */
@NoRepositoryBean
public interface JRepository<E, ID> extends PagingAndSortingRepository<E, ID> {

    /*
     * For provider
     */

    JSqlClient sql();

    ImmutableType type();

    Class<E> entityType();

    /**
     * Will be removed in 0.9
     *
     * <ul>
     *     <li>
     *          To query the `Page&lt;T&gt;` of jimmer,
     *          please <pre>{@code
     *              sql()
     *              .createQuery(table)
     *              ...select(...)
     *              .fetchPage(pageable.getPageNumber(), pageable.getPageSize())
     *          }</pre>
     *     </li>
     *     <li>
     *         To query the `Page&lt;T&gt;` of spring-data,
     *          please <pre>{@code
     *              sql()
     *              .createQuery(table)
     *              ...select(...)
     *              .fetchPage(pageable.getPageNumber(), pageable.getPageSize(), SpringPageFactory.create())
     *          }</pre>
     *     </li>
     * </ul>
     */
    @Deprecated
    Pager pager(Pageable pageable);

    /**
     * Will be removed in 1.0
     *
     * <ul>
     *     <li>
     *          To query the `Page&lt;T&gt;` of jimmer,
     *          please <pre>{@code
     *              sql()
     *              .createQuery(table)
     *              ...select(...)
     *              .fetchPage(pageIndex, pageSize)
     *          }</pre>
     *     </li>
     *     <li>
     *         To query the `Page&lt;T&gt;` of spring-data,
     *          please <pre>{@code
     *              sql()
     *              .createQuery(table)
     *              ...select(...)
     *              .fetchPage(pageIndex, pageSize, SpringPageFactory.create())
     *          }</pre>
     *     </li>
     * </ul>
     */
    @Deprecated
    Pager pager(int pageIndex, int pageSize);

    /*
     * For consumer
     */

    E findNullable(ID id);

    E findNullable(ID id, Fetcher<E> fetcher);

    @NotNull
    @Override
    default Optional<E> findById(@NotNull ID id) {
        return Optional.ofNullable(findNullable(id));
    }

    @NotNull
    default Optional<E> findById(ID id, Fetcher<E> fetcher) {
        return Optional.ofNullable(findNullable(id, fetcher));
    }

    @AliasFor("findAllById")
    List<E> findByIds(Iterable<ID> ids);

    @AliasFor("findByIds")
    @NotNull
    @Override
    default List<E> findAllById(@NotNull Iterable<ID> ids) {
        return findByIds(ids);
    }

    List<E> findByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    Map<ID, E> findMapByIds(Iterable<ID> ids);

    Map<ID, E> findMapByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    @NotNull
    @Override
    List<E> findAll();

    List<E> findAll(TypedProp.Scalar<?, ?> ... sortedProps);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull
    @Override
    List<E> findAll(@NotNull Sort sort);

    List<E> findAll(Fetcher<E> fetcher, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher);

    Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... sortedProps);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);
    
    Page<E> findAll(int pageIndex, int pageSize, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, Sort sort);

    @NotNull
    @Override
    Page<E> findAll(@NotNull Pageable pageable);
    
    Page<E> findAll(Pageable pageable, Fetcher<E> fetcher);

    @Override
    default boolean existsById(@NotNull ID id) {
        return findNullable(id) != null;
    }

    @Override
    long count();

    @NotNull
    default E insert(@NotNull E entity) {
        return save(entity, SaveMode.INSERT_ONLY).getModifiedEntity();
    }

    @NotNull
    default E insert(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_ONLY).getModifiedEntity();
    }

    @NotNull
    default E insertIfAbsent(@NotNull E entity) {
        return save(entity, SaveMode.INSERT_IF_ABSENT).getModifiedEntity();
    }

    @NotNull
    default E insertIfAbsent(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT).getModifiedEntity();
    }

    @NotNull
    default E update(@NotNull E entity) {
        return save(entity, SaveMode.UPDATE_ONLY).getModifiedEntity();
    }
    @NotNull
    default E update(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.UPDATE_ONLY).getModifiedEntity();
    }

    @NotNull
    @Override
    default <S extends E> S save(@NotNull S entity) {
        return saveCommand(entity).execute().getModifiedEntity();
    }

    @NotNull
    default <S extends E> SimpleSaveResult<S> save(@NotNull S entity, SaveMode mode) {
        return saveCommand(entity).setMode(mode).execute();
    }

    @NotNull
    default <S extends E> SimpleSaveResult<S> save(@NotNull S entity, AssociatedSaveMode associatedMode) {
        return saveCommand(entity).setAssociatedModeAll(associatedMode).execute();
    }

    @NotNull
    default <S extends E> SimpleSaveResult<S> save(@NotNull S entity, SaveMode mode, AssociatedSaveMode associatedMode) {
        return saveCommand(entity).setMode(mode).setAssociatedModeAll(associatedMode).execute();
    }

    @NotNull
    default E save(@NotNull Input<E> input) {
        return saveCommand(input.toEntity()).execute().getModifiedEntity();
    }

    @NotNull
    default SimpleSaveResult<E> save(@NotNull Input<E> input, SaveMode mode) {
        return saveCommand(input.toEntity()).setMode(mode).execute();
    }

    @NotNull
    default SimpleSaveResult<E> save(@NotNull Input<E> input, AssociatedSaveMode associatedMode) {
        return saveCommand(input.toEntity()).setAssociatedModeAll(associatedMode).execute();
    }

    @NotNull
    default SimpleSaveResult<E> save(@NotNull Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return saveCommand(input.toEntity()).setMode(mode).setAssociatedModeAll(associatedMode).execute();
    }

    /**
     * <p>Note: The 'merge' of 'Jimmer' and the 'merge' of 'JPA' are completely different concepts!</p>
     *
     * <p>For associated objects, only insert or update operations are executed.
     * The parent object never dissociates the child objects.</p>
     */
    default <S extends E> SimpleSaveResult<S> merge(@NotNull S entity) {
        return saveCommand(entity).setAssociatedModeAll(AssociatedSaveMode.MERGE).execute();
    }

    /**
     * <p>Note: The 'merge' of 'Jimmer' and the 'merge' of 'JPA' are completely different concepts!</p>
     *
     * <p>For associated objects, only insert or update operations are executed.
     * The parent object never dissociates the child objects.</p>
     */
    default SimpleSaveResult<E> merge(@NotNull Input<E> input) {
        return saveCommand(input.toEntity()).setAssociatedModeAll(AssociatedSaveMode.MERGE).execute();
    }

    /**
     * This method will be deleted in 0.9.
     * <p>Note: The 'merge' of 'Jimmer' and the 'merge' of 'JPA' are completely different concepts!</p>
     *
     * <p>For associated objects, only insert or update operations are executed.
     * The parent object never dissociates the child objects.</p>
     */
    @Deprecated
    default <S extends E> SimpleSaveResult<S> merge(@NotNull S entity, SaveMode mode) {
        return saveCommand(entity).setAssociatedModeAll(AssociatedSaveMode.MERGE).setMode(mode).execute();
    }

    /**
     * This method will be deleted in 0.9.
     *
     * <p>Note: The 'merge' of 'Jimmer' and the 'merge' of 'JPA' are completely different concepts!</p>
     *
     * <p>For associated objects, only insert or update operations are executed.
     * The parent object never dissociates the child objects.</p>
     */
    @Deprecated
    default SimpleSaveResult<E> merge(@NotNull Input<E> input, SaveMode mode) {
        return saveCommand(input.toEntity()).setAssociatedModeAll(AssociatedSaveMode.MERGE).setMode(mode).execute();
    }

    /**
     * This method will be deleted in 0.9.
     * For associated objects, only insert operations are executed.
     */
    @Deprecated
    default <S extends E> SimpleSaveResult<S> append(@NotNull S entity) {
        return saveCommand(entity).setAssociatedModeAll(AssociatedSaveMode.APPEND).execute();
    }

    /**
     * This method will be deleted in 0.9
     * For associated objects, only insert operations are executed.
     */
    @Deprecated
    default SimpleSaveResult<E> append(@NotNull Input<E> input) {
        return saveCommand(input.toEntity()).setAssociatedModeAll(AssociatedSaveMode.APPEND).execute();
    }

    /**
     * This method will be deleted in 0.9
     * For associated objects, only insert operations are executed.
     */
    @Deprecated
    default <S extends E> SimpleSaveResult<S> append(@NotNull S entity, SaveMode mode) {
        return saveCommand(entity).setAssociatedModeAll(AssociatedSaveMode.APPEND).setMode(mode).execute();
    }

    /**
     * This method will be deleted in 0.9
     * For associated objects, only insert operations are executed.
     */
    @Deprecated
    default SimpleSaveResult<E> append(@NotNull Input<E> input, SaveMode mode) {
        return saveCommand(input.toEntity()).setAssociatedModeAll(AssociatedSaveMode.APPEND).setMode(mode).execute();
    }

    @NotNull
    SimpleEntitySaveCommand<E> saveCommand(@NotNull Input<E> input);

    @NotNull
    <S extends E> SimpleEntitySaveCommand<S> saveCommand(@NotNull S entity);

    /**
     * Replaced by saveEntities, will be removed in 1.0
     */
    @Deprecated
    @Override
    default <S extends E> Iterable<S> saveAll(Iterable<S> entities) {
        return saveEntities(entities);
    }

    @NotNull
    default <S extends E> Iterable<S> saveEntities(@NotNull Iterable<S> entities) {
        return saveEntitiesCommand(entities)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveEntities(@NotNull Iterable<S> entities, SaveMode mode) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveEntities(@NotNull Iterable<S> entities, AssociatedSaveMode associatedMode) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveEntities(
            @NotNull Iterable<S> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveInputs(@NotNull Iterable<Input<S>> entities) {
        return saveInputsCommand(entities)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveInputs(@NotNull Iterable<Input<S>> entities, SaveMode mode) {
        return saveInputsCommand(entities)
                .setMode(mode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveInputs(@NotNull Iterable<Input<S>> entities, AssociatedSaveMode associatedMode) {
        return saveInputsCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    default <S extends E> Iterable<S> saveInputs(
            @NotNull Iterable<Input<S>> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @NotNull
    <S extends E> BatchEntitySaveCommand<S> saveEntitiesCommand(@NotNull Iterable<S> entities);

    @NotNull
    default <S extends E> BatchEntitySaveCommand<S> saveInputsCommand(@NotNull Iterable<Input<S>> inputs) {
        return saveEntitiesCommand(CollectionUtils.map(inputs, Input::toEntity));
    }

    @Override
    default void delete(@NotNull E entity) {
        delete(entity, DeleteMode.AUTO);
    }

    int delete(@NotNull E entity, DeleteMode mode);

    @Override
    default void deleteAll(@NotNull Iterable<? extends E> entities) {
        deleteAll(entities, DeleteMode.AUTO);
    }

    int deleteAll(@NotNull Iterable<? extends E> entities, DeleteMode mode);

    @Override
    default void deleteById(@NotNull ID id) {
        deleteById(id, DeleteMode.AUTO);
    }

    int deleteById(@NotNull ID id, DeleteMode mode);
    
    @AliasFor("deleteAllById")
    default void deleteByIds(Iterable<? extends ID> ids) {
        deleteByIds(ids, DeleteMode.AUTO);
    }

    @AliasFor("deleteByIds")
    @Override
    default void deleteAllById(@NotNull Iterable<? extends ID> ids) {
        deleteByIds(ids, DeleteMode.AUTO);
    }

    @AliasFor("deleteAllById")
    int deleteByIds(Iterable<? extends ID> ids, DeleteMode mode);

    @Override
    void deleteAll();

    <V extends View<E>> Viewer<E, ID, V> viewer(Class<V> viewType);

    @Deprecated
    interface Pager {

        <T> Page<T> execute(ConfigurableRootQuery<?, T> query);
    }

    interface Viewer<E, ID, V extends View<E>> {

        V findNullable(ID id);

        List<V> findByIds(Iterable<ID> ids);

        Map<ID, V> findMapByIds(Iterable<ID> ids);

        List<V> findAll();

        List<V> findAll(TypedProp.Scalar<?, ?> ... sortedProps);

        List<V> findAll(Sort sort);

        Page<V> findAll(Pageable pageable);

        Page<V> findAll(int pageIndex, int pageSize);

        Page<V> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... sortedProps);

        Page<V> findAll(int pageIndex, int pageSize, Sort sort);
    }
}
