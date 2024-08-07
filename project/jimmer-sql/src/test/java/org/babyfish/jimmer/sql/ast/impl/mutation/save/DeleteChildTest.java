package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.flat.ProvinceProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class DeleteChildTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectExceptBySimpleInPredicate() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(manningId, graphQLInActionId1),
                                            new Tuple2<>(manningId, graphQLInActionId2)
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID in (" +
                                        "--->select ID " +
                                        "--->from BOOK " +
                                        "--->where STORE_ID = ? and ID not in (?, ?)" +
                                        ")"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and ID not in (?, ?)"
                        );
                        it.variables(manningId, graphQLInActionId1, graphQLInActionId2);
                    });
                    ctx.value("1");
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicate() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID in (" +
                                        "--->select ID " +
                                        "--->from BOOK " +
                                        "--->where " +
                                        "--->--->STORE_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(STORE_ID, ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))" +
                                        ")"
                        );
                        it.variables(
                                oreillyId, manningId,
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK where " +
                                        "--->STORE_ID in (?, ?) " +
                                        "and " +
                                        "--->(STORE_ID, ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                oreillyId, manningId,
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.value("8");
                }
        );
    }

    @Test
    public void testDisconnectExceptByBatch() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> it.setDialect(new H2Dialect())),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID in (" +
                                        "--->select ID " +
                                        "--->from BOOK " +
                                        "--->where STORE_ID = ? and not (ID = any(?))" +
                                        ")"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and not (ID = any(?))"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.value("8");
                }
        );
    }

    @Test
    public void testDisconnectExceptBySelect() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> {
                                it.setMaxMutationSubQueryDepth(1);
                                it.setDialect(new H2Dialect());
                            }),
                            con,
                            BookProps.STORE.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(oreillyId, learningGraphQLId1),
                                            new Tuple2<>(oreillyId, effectiveTypeScriptId1),
                                            new Tuple2<>(oreillyId, programmingTypeScriptId1),
                                            new Tuple2<>(manningId, graphQLInActionId1)
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "inner join BOOK tb_2_ on tb_1_.BOOK_ID = tb_2_.ID " +
                                        "where " +
                                        "--->tb_2_.STORE_ID = any(?) " +
                                        "and " +
                                        "--->(tb_2_.STORE_ID, tb_2_.ID) not in ((?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                new Object[] { oreillyId, manningId },
                                oreillyId, learningGraphQLId1,
                                oreillyId, effectiveTypeScriptId1,
                                oreillyId, programmingTypeScriptId1,
                                manningId, graphQLInActionId1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where BOOK_ID = ? and AUTHOR_ID = ?"
                        );
                        it.batchVariables(0, graphQLInActionId2, sammerId);
                        it.batchVariables(1, graphQLInActionId3, sammerId);
                        it.batchVariables(2, learningGraphQLId2, alexId);
                        it.batchVariables(3, learningGraphQLId2, eveId);
                        it.batchVariables(4, learningGraphQLId3, alexId);
                        it.batchVariables(5, learningGraphQLId3, eveId);
                        it.batchVariables(6, effectiveTypeScriptId2, danId);
                        it.batchVariables(7, effectiveTypeScriptId3, danId);
                        it.batchVariables(8, programmingTypeScriptId2, borisId);
                        it.batchVariables(9, programmingTypeScriptId3, borisId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where STORE_ID = ? and not (ID = any(?))"
                        );
                        it.batchVariables(
                                0, oreillyId, new Object[] {
                                        learningGraphQLId1,
                                        effectiveTypeScriptId1,
                                        programmingTypeScriptId1
                                }
                        );
                        it.batchVariables(
                                1, manningId, new Object[] {
                                        graphQLInActionId1
                                }
                        );
                    });
                    ctx.value("8");
                }
        );
    }

    @Test
    public void testDisconnectExceptBySimpleInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("001")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(1).setC(1))
                                            )
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->select ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C " +
                                        "--->from ORDER_ITEM " +
                                        "--->where " +
                                        "--->--->(FK_ORDER_X, FK_ORDER_Y) = (?, ?) " +
                                        "--->and " +
                                        "--->(ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) <> (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y) = (?, ?) " +
                                        "and " +
                                        "--->(ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) <> (?, ?, ?)"
                        );
                        it.variables(
                                "001", "001", 1, 1, 1
                        );
                    });
                    ctx.value("1");
                }
        );
    }

    @Test
    public void testDisconnectExceptByComplexInPredicateAndEmbedded() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            OrderItemProps.ORDER.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    Arrays.asList(
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("001")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(1).setC(1))
                                            ),
                                            new Tuple2<>(
                                                    Objects.createOrderId(draft -> draft.setX("001").setY("002")),
                                                    Objects.createOrderItemId(draft -> draft.setA(1).setB(2).setC(1))
                                            )
                                    )
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM_PRODUCT_MAPPING " +
                                        "where (" +
                                        "--->FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C" +
                                        ") in (" +
                                        "--->select ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C " +
                                        "--->from ORDER_ITEM " +
                                        "--->where " +
                                        "--->--->(FK_ORDER_X, FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "--->and " +
                                        "--->--->(FK_ORDER_X, FK_ORDER_Y, ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) " +
                                        "--->--->not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))" +
                                        ")"
                        );
                        it.variables(
                                "001", "001", "001", "002",
                                "001", "001", 1, 1, 1,
                                "001", "002", 1, 2, 1
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from ORDER_ITEM " +
                                        "where " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y) in ((?, ?), (?, ?)) " +
                                        "and " +
                                        "--->(FK_ORDER_X, FK_ORDER_Y, ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C) " +
                                        "--->not in ((?, ?, ?, ?, ?), (?, ?, ?, ?, ?))"
                        );
                        it.variables(
                                "001", "001", "001", "002",
                                "001", "001", 1, 1, 1,
                                "001", "002", 1, 2, 1
                        );
                    });
                    ctx.value("2");
                }
        );
    }

    @Test
    public void testDisconnectTreeExcept() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> it.setMaxMutationSubQueryDepth(4)),
                            con,
                            ProvinceProps.COUNTRY.unwrap(),
                            DissociateAction.DELETE
                    ).disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2),
                                    new Tuple2<>(2L, 4)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from COMPANY " +
                                        "where STREET_ID in (" +
                                        "--->select ID from STREET where CITY_ID in (" +
                                        "--->--->select ID from CITY where PROVINCE_ID in (" +
                                        "--->--->--->select ID " +
                                        "--->--->--->from PROVINCE " +
                                        "--->--->--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        "--->--->)" +
                                        "--->)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET where CITY_ID in (" +
                                        "--->select ID from CITY where PROVINCE_ID in (" +
                                        "--->--->select ID " +
                                        "--->--->from PROVINCE " +
                                        "--->--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        "--->)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY where PROVINCE_ID in (" +
                                        "--->select ID " +
                                        "--->from PROVINCE " +
                                        "--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                }
        );
    }
}
