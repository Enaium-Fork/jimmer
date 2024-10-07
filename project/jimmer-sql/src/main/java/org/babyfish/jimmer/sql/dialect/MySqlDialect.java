package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

/**
 * For MySQL or TiDB
 */
public class MySqlDialect extends DefaultDialect {

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .space()
                .sql("limit ")
                .variable(ctx.getOffset())
                .sql(", ")
                .variable(ctx.getLimit());
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(true, UpdateJoin.From.UNNECESSARY);
    }

    @Override
    public boolean isDeletedAliasRequired() {
        return true;
    }

    @Override
    public String sqlType(Class<?> elementType) {
        if (elementType == String.class) {
            return "varchar";
        }
        if (elementType == UUID.class) {
            return "char(36)";
        }
        if (elementType == boolean.class) {
            return "boolean";
        }
        if (elementType == byte.class) {
            return "tinyint";
        }
        if (elementType == short.class) {
            return "smallint";
        }
        if (elementType == int.class) {
            return "int";
        }
        if (elementType == long.class) {
            return "bigint";
        }
        if (elementType == float.class) {
            return "float";
        }
        if (elementType == double.class) {
            return "double";
        }
        if (elementType == BigDecimal.class) {
            return "decimal";
        }
        if (elementType == java.sql.Date.class || elementType == LocalDate.class) {
            return "date";
        }
        if (elementType == java.sql.Time.class || elementType == LocalTime.class) {
            return "datetime";
        }
        if (elementType == OffsetTime.class) {
            return "datetime";
        }
        if (elementType == java.util.Date.class || elementType == java.sql.Timestamp.class) {
            return "timestamp";
        }
        if (elementType == LocalDateTime.class) {
            return "timestamp";
        }
        if (elementType == OffsetDateTime.class || elementType == ZonedDateTime.class) {
            return "timestamp";
        }
        return null;
    }

    @Override
    public boolean isUpsertWithMultipleUniqueConstraintSupported() {
        return false;
    }

    @Override
    public boolean isIdFetchableByKeyUpdate() {
        return true;
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public void update(UpdateContext ctx) {
        if (!ctx.isUpdatedByKey()) {
            super.update(ctx);
            return;
        }
        ctx
                .sql("update ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.SET)
                .separator()
                .appendId()
                .sql(" = last_insert_id(")
                .appendId()
                .sql(")")
                .appendAssignments()
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.WHERE)
                .appendPredicates()
                .leave();
    }

    @Override
    public void upsert(UpsertContext ctx) {
        if (!ctx.hasUpdatedColumns() && !ctx.hasGeneratedId()) {
            ctx.sql("insert ignore into ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertedColumns("")
                    .leave()
                    .enter(AbstractSqlBuilder.ScopeType.VALUES)
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertingValues()
                    .leave()
                    .leave();
        } else {
            ctx.sql("insert into ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertedColumns("")
                    .leave()
                    .enter(AbstractSqlBuilder.ScopeType.VALUES)
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertingValues()
                    .leave()
                    .leave()
                    .sql(" on duplicate key update ")
                    .enter(AbstractSqlBuilder.ScopeType.COMMA);
            if (ctx.hasGeneratedId()) {
                ctx.separator()
                        .sql(FAKE_UPDATE_COMMENT)
                        .sql(" ")
                        .appendGeneratedId()
                        .sql(" = ")
                        .sql("last_insert_id(")
                        .appendGeneratedId()
                        .sql(")");
            }
            if (ctx.hasUpdatedColumns()) {
                ctx.separator().appendUpdatingAssignments("values(", ")");
            }
            ctx.leave();
        }
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(\n" +
                "\tID bigint unsigned not null auto_increment primary key,\n" +
                "\tIMMUTABLE_TYPE varchar(128),\n" +
                "\tIMMUTABLE_PROP varchar(128),\n" +
                "\tCACHE_KEY varchar(64) not null,\n" +
                "\tREASON varchar(32)\n" +
                ") engine=innodb";
    }
}
