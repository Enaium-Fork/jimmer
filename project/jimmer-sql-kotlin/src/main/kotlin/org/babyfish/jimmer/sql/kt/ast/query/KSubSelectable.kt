package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.rowCount
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTable

interface KSubSelectable {

    fun <T: Any> select(
        expression: KNonNullExpression<T>
    ): KConfigurableSubQuery.NonNull<T>

    fun <T: Any> select(
        expression: KNullableExpression<T>
    ): KConfigurableSubQuery.Nullable<T>

    fun <T: Any> select(
        table: KNonNullTable<T>
    ): KConfigurableSubQuery.NonNull<T>

    fun <T: Any> select(
        table: KNullableTable<T>
    ): KConfigurableSubQuery.Nullable<T>

    fun <T1, T2> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KConfigurableSubQuery.NonNull<Tuple2<T1, T2>>

    fun <T1, T2, T3> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KConfigurableSubQuery.NonNull<Tuple3<T1, T2, T3>>

    fun <T1, T2, T3, T4> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>
    ): KConfigurableSubQuery.NonNull<Tuple4<T1, T2, T3, T4>>

    fun <T1, T2, T3, T4, T5> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>
    ): KConfigurableSubQuery.NonNull<Tuple5<T1, T2, T3, T4, T5>>

    fun <T1, T2, T3, T4, T5, T6> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>
    ): KConfigurableSubQuery.NonNull<Tuple6<T1, T2, T3, T4, T5, T6>>

    fun <T1, T2, T3, T4, T5, T6, T7> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>
    ): KConfigurableSubQuery.NonNull<Tuple7<T1, T2, T3, T4, T5, T6, T7>>

    fun <T1, T2, T3, T4, T5, T6, T7, T8> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>
    ): KConfigurableSubQuery.NonNull<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>

    fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>,
        selection9: Selection<T9>
    ): KConfigurableSubQuery.NonNull<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>

    fun selectCount(): KConfigurableSubQuery.NonNull<Long> =
        select(rowCount())
}