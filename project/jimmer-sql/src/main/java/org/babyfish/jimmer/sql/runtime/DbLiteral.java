package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public interface DbLiteral {

    Class<?> getType();

    default void render(StringBuilder builder, JSqlClientImplementor sqlClient) {
        builder.append('?');
    }

    void renderValue(StringBuilder builder);

    void renderToComment(StringBuilder builder);

    void setParameter(PreparedStatement stmt, ParameterIndex index, JSqlClientImplementor sqlClient) throws Exception;

    class DbNull implements DbLiteral {

        private final Class<?> type;

        public DbNull(Class<?> type) {
            this.type = type;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public void renderValue(StringBuilder builder) {
            builder.append("null");
        }

        @Override
        public void renderToComment(StringBuilder builder) {
            builder.append("<null: ").append(type.getSimpleName()).append('>');
        }

        @Override
        public void setParameter(PreparedStatement stmt, ParameterIndex index, JSqlClientImplementor sqlClient) throws Exception {
            stmt.setNull(index.get(), JdbcTypes.toJdbcType(type, sqlClient.getDialect()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DbNull dbNull = (DbNull) o;
            return type.equals(dbNull.type);
        }

        @Override
        public String toString() {
            return "DbNull{" +
                    "type=" + type.getName() +
                    '}';
        }
    }

    class DbValue implements DbLiteral {

        private final ImmutableProp prop;

        private final Object value;

        private final boolean converted;

        public DbValue(ImmutableProp prop, Object value, boolean converted) {
            if (value instanceof DbLiteral) {
                throw new IllegalArgumentException("value cannot be DbLiteral");
            }
            this.prop = prop;
            this.value = value;
            this.converted = converted;
        }

        @Override
        public Class<?> getType() {
            return value != null ? value.getClass() : prop.getReturnClass();
        }

        @Override
        public void render(StringBuilder builder, JSqlClientImplementor sqlClient) {
            builder.append('?');
            String suffix = sqlClient.getDialect().getJsonLiteralSuffix();
            if (value != null && suffix != null) {
                ScalarProvider<?, ?> scalarProvider = sqlClient.getScalarProvider(prop);
                if (scalarProvider != null && scalarProvider.isJsonScalar()) {
                    builder.append(' ').append(suffix);
                }
            }
        }

        @Override
        public void renderValue(StringBuilder builder) {
            if (value instanceof Number) {
                builder.append("null");
            } else {
                builder
                        .append('\'')
                        .append(value.toString().replace("'", "''"))
                        .append('\'');
            }
        }

        @Override
        public void renderToComment(StringBuilder builder) {
            builder.append(value.toString());
        }

        @Override
        public void setParameter(PreparedStatement stmt, ParameterIndex index, JSqlClientImplementor sqlClient) throws Exception {
            Object value = this.value;
            ScalarProvider<Object, Object> scalarProvider = null;
            if (value != null && !converted) {
                scalarProvider = sqlClient.getScalarProvider(prop);
                if (scalarProvider != null) {
                    try {
                        value = scalarProvider.toSql(value);
                    } catch (Exception ex) {
                        throw new ExecutionException(
                                "The value \"" +
                                        value +
                                        "\" cannot be converted by the scalar provider \"" +
                                        scalarProvider +
                                        "\""
                        );
                    }
                }
            }
            if (value == null) {
                stmt.setNull(
                        index.get(),
                        JdbcTypes.toJdbcType(
                                scalarProvider != null ? scalarProvider.getSqlType() : prop.getReturnClass(),
                                sqlClient.getDialect()
                        )
                );
            } else {
                stmt.setObject(
                        index.get(),
                        value,
                        JdbcTypes.toJdbcType(value.getClass(), sqlClient.getDialect())
                );
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DbValue dbValue = (DbValue) o;

            if (converted != dbValue.converted) return false;
            if (!prop.equals(dbValue.prop)) return false;
            return value.equals(dbValue.value);
        }

        @Override
        public int hashCode() {
            int result = prop.hashCode();
            result = 31 * result + value.hashCode();
            result = 31 * result + (converted ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "DbValue{" +
                    "prop=" + prop +
                    ", value=" + value +
                    ", converted=" + converted +
                    '}';
        }
    }

    static Object unwrap(Object value) {
        if (value instanceof DbNull) {
            return null;
        }
        if (value instanceof DbValue) {
            return ((DbValue)value).value;
        }
        return value;
    }
}


