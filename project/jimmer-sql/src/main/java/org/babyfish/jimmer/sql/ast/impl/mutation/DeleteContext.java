package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.MutationPath;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.Map;

class DeleteContext extends MutationContext {

    final DeleteContext parent;

    final DeleteOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final ImmutableProp backProp;

    private Boolean logicalDeleted;

    DeleteContext(
            DeleteOptions options,
            Connection con,
            MutationTrigger trigger,
            Map<AffectedTable, Integer> affectedRowCountMap,
            MutationPath path
    ) {
        super(path);
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
        this.affectedRowCountMap = affectedRowCountMap;
        this.backProp = mappedBy;
    }

    private DeleteContext(DeleteContext parent, ImmutableProp prop, ImmutableProp backProp) {
        super(prop != null ? parent.path.to(prop) : parent.path.backFrom(backProp));
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
            if (!backProp.isAssociation(TargetLevel.ENTITY) ||
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
        this.affectedRowCountMap = parent.affectedRowCountMap;
        if (prop != null) {
            this.backProp = null;
        } else {
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

    boolean isLogicalDeleted() {
        Boolean ld = logicalDeleted;
        if (ld == null) {
            LogicalDeletedInfo info = path.getType().getLogicalDeletedInfo();
            switch (options.getMode()) {
                case LOGICAL:
                    if (info == null) {
                        throw new IllegalArgumentException(
                                "Cannot logically delete the object whose type is \"" +
                                        path.getType() +
                                        "\" because that type does not support logical deletion"
                        );
                    }
                    ld = true;
                    break;
                case PHYSICAL:
                    ld = false;
                    break;
                default:
                    ld = info != null;
                    break;
            }
            this.logicalDeleted = ld;
        }
        return ld;
    }

    @NotNull
    public DeleteOptions getOptions() {
        return options;
    }
}
