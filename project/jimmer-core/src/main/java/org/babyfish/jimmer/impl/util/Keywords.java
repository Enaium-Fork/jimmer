package org.babyfish.jimmer.impl.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Keywords {

    public static Set<String> ILLEGAL_PROP_NAMES = Collections.unmodifiableSet(
            new HashSet<>(
                    Arrays.asList(
                            "hashCode",
                            "equals",
                            "toString",

                            // DraftSpi
                            "__isLoaded",
                            "__isVisible",
                            "__get",
                            "__hashCode",
                            "__equals",
                            "__type",

                            "__unload",
                            "__set",
                            "__show",
                            "__draftContext",
                            "__resolve",

                            // View, Input
                            "toEntity",

                            // Specification
                            "applyTo", "entityType",

                            // AbstractTypedTable
                            "immutableType",
                            "eq",
                            "null",
                            "notNull",
                            "count",
                            "get",
                            "associatedId",
                            "__get",
                            "__getAssociatedId",
                            "join",
                            "inverseJoin",
                            "inverseGetAssociatedId",
                            "fetch",
                            "asTableEx",
                            "__parent",
                            "__prop",
                            "__weakJoinHandle",
                            "__isInverse",
                            "__unwrap",
                            "__resolve",
                            "__beforeJoin",
                            "__disableJoin",
                            "joinOperation",
                            "__currentJoinType",
                            "__refEquals",

                            // AbstractTypedFetcher
                            "allScalarFields",
                            "allTableFields",
                            "add",
                            "remove",
                            "createFetcher",
                            "javaClass",
                            "immutableType",
                            "fieldMap",
                            "__isSimpleFetcher",
                            "__unresolvedFieldMap",
                            "__shownPropIds",
                            "__hiddenPropIds",

                            // KNonNullTableEx
                            "get",
                            "associatedId",
                            "join",
                            "joinReference",
                            "joinList",
                            "outerJoin",
                            "outerJoinReference",
                            "outerJoinList",
                            "inverseJoin",
                            "inverseJoinReference",
                            "inverseJoinList",
                            "inverseOuterJoin",
                            "inverseOuterJoinReference",
                            "inverseOuterJoinList",
                            "weakJoin",
                            "weakOuterJoin",
                            "asTableEx"
                    )
            )
    );

    private Keywords() {}
}
