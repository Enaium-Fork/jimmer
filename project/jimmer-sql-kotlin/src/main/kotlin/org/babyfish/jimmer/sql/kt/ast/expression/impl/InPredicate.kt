package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery

internal class InCollectionPredicate(
    private val nullable: Boolean,
    private val negative: Boolean,
    private var expression: KExpression<*>,
    private val values: Collection<*>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        InCollectionPredicate(nullable, !negative, expression, values)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        ComparisonPredicates.renderIn(
            nullable,
            negative,
            expression as ExpressionImplementor<*>,
            values,
            builder
        )
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expression)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        expression = ctx.resolveVirtualPredicate(expression)
        return this
    }
}

internal class InExpressionCollectionPredicate(
    private val negative: Boolean,
    private var expression: KExpression<*>,
    private var operands: Collection<KExpression<*>>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        InExpressionCollectionPredicate(!negative, expression, operands)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
    }

    @Suppress("UNCHECKED_CAST")
    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        ComparisonPredicates.renderExpressionIn(
            negative,
            expression as ExpressionImplementor<*>,
            operands as Collection<Expression<*>>,
            builder
        )
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expression) ||
        hasVirtualPredicate(operands)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        expression = ctx.resolveVirtualPredicate(expression)
        operands = operands.map { ctx.resolveVirtualPredicate(it) }
        return this
    }
}

internal class InSubQueryPredicate(
    private val negative: Boolean,
    private var expression: KExpression<*>,
    private var subQuery: KTypedSubQuery<*>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        InSubQueryPredicate(!negative, expression, subQuery)

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {
        (expression as Ast).accept(visitor)
        (subQuery as Ast).accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        (expression as Ast).renderTo(builder)
        builder.sql(if (negative) " not in " else " in ")
        (subQuery as Ast).renderTo(builder)
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expression) || hasVirtualPredicate(subQuery)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast {
        expression = ctx.resolveVirtualPredicate(expression)
        subQuery = ctx.resolveVirtualPredicate(subQuery)
        return this
    }
}