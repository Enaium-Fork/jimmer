package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class InCollectionPredicate(
    private val negative: Boolean,
    private val expression: KExpression<*>,
    private val values: Collection<*>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        InCollectionPredicate(!negative, expression, values)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        if (values.isEmpty()) {
            builder.sql(if (negative) "1 = 1" else "1 = 0")
        } else {
            (expression as Ast).renderTo(builder)
            builder.sql(if (negative) " not in (" else " in (")
            var sp = ""
            for (value in values) {
                builder.sql(sp)
                sp = ", "
                builder.variable(value)
            }
            builder.sql(")")
        }
    }
}

internal class InSubQueryPredicate(
    private val negative: Boolean,
    private val expression: KExpression<*>,
    private val subQuery: KTypedSubQuery<*>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        InSubQueryPredicate(!negative, expression, subQuery)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
        (subQuery as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        (expression as Ast).renderTo(builder)
        builder.sql(if (negative) " not in " else " in ")
        (subQuery as Ast).renderTo(builder)
    }
}