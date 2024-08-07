package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.sql.Connection;
import java.util.Map;

class DeleteContext {

    final DeleteContext parent;

    final DeleteOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final boolean triggerSubmitImmediately;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final MutationPath path;

    final ImmutableProp backProp;

    DeleteContext(
            DeleteOptions options,
            Connection con,
            MutationTrigger trigger,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap,
            MutationPath path
    ) {
        ImmutableProp mappedBy = path.getProp() != null ? path.getProp().getMappedBy() : null;
        if (mappedBy != null && !mappedBy.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The property \"" +
                            path.getProp() +
                            "\" does not reference child table"
            );
        }
        this.parent = null;
        this.options = options;
        this.con = con;
        this.trigger = trigger;
        this.triggerSubmitImmediately = triggerSubmitImmediately;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = path;
        this.backProp = mappedBy;
    }

    private DeleteContext(DeleteContext parent, ImmutableProp prop, ImmutableProp backProp) {
        if (prop != null) {
            if (!prop.isAssociation(TargetLevel.ENTITY) ||
                    (!prop.isColumnDefinition() && !prop.isMiddleTableDefinition())) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" is not association property with column defined or middle table"
                );
            }
        } else {
            if (!backProp.isReference(TargetLevel.ENTITY) ||
                    (!backProp.isColumnDefinition() && !backProp.isMiddleTableDefinition())) {
                throw new IllegalArgumentException(
                        "The back property \"" +
                                backProp +
                                "\" is not association property with column defined or middle table"
                );
            }
        }
        this.parent = parent;
        this.options = parent.options;
        this.con = parent.con;
        this.trigger = parent.trigger;
        this.triggerSubmitImmediately = parent.triggerSubmitImmediately;
        this.affectedRowCountMap = parent.affectedRowCountMap;
        if (prop != null) {
            this.path = parent.path.to(prop);
            this.backProp = null;
        } else {
            this.path = parent.path.backFrom(backProp);
            this.backProp = backProp;
        }
    }

    DeleteContext propOf(ImmutableProp prop) {
        if (prop.getMappedBy() != null) {
            return new DeleteContext(this, null, prop.getMappedBy());
        }
        return new DeleteContext(this, prop, null);
    }

    DeleteContext backPropOf(ImmutableProp backProp) {
        if (backProp.getMappedBy() != null) {
            return new DeleteContext(this, backProp.getMappedBy(), null);
        }
        return new DeleteContext(this, null, backProp);
    }
}
