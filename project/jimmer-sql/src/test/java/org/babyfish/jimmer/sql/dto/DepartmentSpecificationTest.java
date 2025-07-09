package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.hr.DepartmentTable;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentSpecification;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentSpecification2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DepartmentSpecificationTest extends AbstractQueryTest {

    @Test
    public void testWithEmptySpecification() {

        DepartmentTable table = DepartmentTable.$;
        DepartmentSpecification specification = new DepartmentSpecification();
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_MILLIS " +
                                    "from DEPARTMENT tb_1_ " +
                                    "where tb_1_.DELETED_MILLIS = ?"
                    );
                }
        );
    }

    @Test
    public void testWithNonEmptySpecification() {

        DepartmentTable table = DepartmentTable.$;
        DepartmentSpecification specification = new DepartmentSpecification();
        specification.setId("3");
        specification.setEmployeeIds(Arrays.asList("4", "5"));
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_MILLIS " +
                                    "from DEPARTMENT tb_1_ " +
                                    "where " +
                                    "--->tb_1_.ID = ? " +
                                    "and " +
                                    "--->exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from EMPLOYEE tb_2_ " +
                                    "--->--->where " +
                                    "--->--->--->tb_2_.DEPARTMENT_ID = tb_1_.ID " +
                                    "--->--->and " +
                                    "--->--->--->tb_2_.ID in (?, ?) " +
                                    "--->--->and " +
                                    "--->--->--->tb_2_.DELETED_MILLIS = ?" +
                                    "--->) " +
                                    "and tb_1_.DELETED_MILLIS = ?"
                    ).variables(3L, 4L, 5L, 0L, 0L);
                }
        );
    }

    @Test
    public void testSpecification2() {
        DepartmentTable table = DepartmentTable.$;
        DepartmentSpecification2 specification = new DepartmentSpecification2();
        specification.setId("3");
        specification.setEmployeeIds(Arrays.asList("4", "5"));
        specification.setEmployeeName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_MILLIS " +
                                    "from DEPARTMENT tb_1_ " +
                                    "where tb_1_.ID = ? " +
                                    "and exists(" +
                                    "--->select 1 " +
                                    "--->from EMPLOYEE tb_2_ " +
                                    "--->where tb_1_.ID = tb_2_.DEPARTMENT_ID " +
                                    "--->and tb_2_.ID in (?, ?) " +
                                    "--->and tb_2_.NAME ilike ? " +
                                    "--->and tb_2_.DELETED_MILLIS = ?" +
                                    ") " +
                                    "and tb_1_.DELETED_MILLIS = ?"
                    ).variables(3L, 4L, 5L, "%bob%", 0L, 0L);
                }
        );
    }
}
