package org.babyfish.jimmer.sql

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.meta.TypedProp

/**
 * Before saving draft, give user a chance to modify it
 *
 * <p>This interface is very similar to another interface
 * [DraftPreProcessor],
 * the differences between the two are as follows:
 * <ul>
 * <li>[DraftPreProcessor] is more conducive to SQL optimization, but has weaker functions</li>
 * <li>This interface has stronger features but is not carp SQL optimized</li>
 * </ul>
 * </p>
 *
 * <p>It also queries the original entity with
 * `id`, `key` and other properties returned by [.dependencies] to help user to decide how to modify draft</p>
 *
 * The default behavior of `save` with `UPDATE_ONLY` or `update` is not querying original entity.
 * However, if [.dependencies] returns some properties which is neither `id` nor `key`,
 * the default behavior will be broken, original entity will be queried even if the save mode is `UPDATE_ONLY`
 *
 * @param <E> The entity type
 * @param <D> The draft type
 *
 * @see DraftPreProcessor
 */
interface DraftInterceptor<E: Any, D : Draft> {

    /**
     * Adjust draft before save
     *
     * <p>
     *  Note, if the other function [beforeSaveAll] is overridden,
     *  this method may not be automatically called by Jimmer.
     *  It depends on the overriding logic of method [beforeSaveAll].
     * </p>
     *
     * @param draft The draft can be modified, `id` and `key` properties cannot be changed, otherwise, exception will be raised.
     * @param original The original object
     *
     *  * null for insert
     *  * non-null for update, with `id`, `key` and other properties
     * returned by [.dependencies]
     */
    fun beforeSave(draft: D, original: E?) {}

    /**
     * In general, developers should override method
     * [beforeSave] instead of the current method.
     *
     * <p>However, in some scenarios, users may execute
     * some additional queries to determine the
     * subsequent logic. In this case, this method
     * can be overridden to avoid the `N+1` query problem
     * to reach better performance.</p>
     */
    fun beforeSaveAll(items: Collection<Item<E, D>>) {
        for (item in items) {
            beforeSave(item.draft, item.original)
        }
    }

    /**
     * Specify which properties of original entity must be loaded
     *
     * <p>Note</p>
     * <ul>
     *  <li>The return value must be stable, It will only be called once, so an unstable return is meaningless</li>
     *  <li>All elements must be properties which is mapped by database field directly</li>
     * </ul>
     *
     * @return The properties must be loaded, can return null.
     */
    fun dependencies(): Collection<TypedProp<E, *>>? {
        return emptyList()
    }

    data class Item<E: Any, D: Draft>(
        val draft: D,
        val original: E?
    )
}
