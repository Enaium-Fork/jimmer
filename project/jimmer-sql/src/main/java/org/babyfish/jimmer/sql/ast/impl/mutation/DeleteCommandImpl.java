package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;

public class DeleteCommandImpl extends AbstractCommandImpl implements DeleteCommand {

    public DeleteCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Iterable<?> ids
    ) {
        super(initialCfg(sqlClient, con, type, ids));
    }

    private DeleteCommandImpl(Cfg cfg) {
        super(cfg);
    }

    @Override
    public DeleteResult execute(Connection con) {
        OptionsImpl options = options();
        return options
                .getSqlClient()
                .getConnectionManager()
                .execute(con == null ? options.con : con, this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private DeleteResult executeImpl(Connection con) {
        OptionsImpl options = options();
        options.getSqlClient().validateMutationConnection(con);
        boolean binLogOnly = options.getSqlClient().getTriggerType() == TriggerType.BINLOG_ONLY;
        Deleter deleter = new Deleter(
                options.argument.type,
                options,
                con,
                binLogOnly ? null : new MutationTrigger(),
                new HashMap<>()
        );
        deleter.addIds((Collection<Object>) options.argument.ids);
        return deleter.execute();
    }

    private static Cfg initialCfg(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Iterable<?> ids
    ) {
        Cfg cfg = new RootCfg(sqlClient, new Argument(type, ids));
        if (con != null) {
            cfg = new ConnectionCfg(cfg, con);
        }
        return cfg;
    }

    @Override
    OptionsImpl createOptions() {
        return new OptionsImpl(cfg);
    }

    private static class Argument {

        final ImmutableType type;

        final Iterable<?> ids;

        private Argument(ImmutableType type, Iterable<?> ids) {
            if (!type.isEntity()) {
                throw new IllegalArgumentException(
                        "Cannot delete object whose type is \"" +
                                type +
                                "\" because that type is not entity"
                );
            }
            Class<?> idClass = type.getIdProp().getElementClass();
            for (Object id : ids) {
                if (Converters.tryConvert(id, idClass) == null) {
                    throw new IllegalArgumentException(
                            "The type of \"" +
                                    type.getIdProp() +
                                    "\" must be \"" +
                                    idClass.getName() +
                                    "\""
                    );
                }
            }
            this.type = type;
            this.ids = ids;
        }
    }

    static class OptionsImpl implements DeleteOptions {

        private final JSqlClientImplementor sqlClient;

        private final Connection con;

        private final DeleteMode mode;

        private final int maxCommandJoinCount;

        private final ExceptionTranslator<?> exceptionTranslator;

        private final Map<ImmutableProp, DissociateAction> dissociateActionMap;

        private boolean dumbBatchAcceptable;

        private final Argument argument;

        OptionsImpl(Cfg cfg) {
            RootCfg rootCfg = cfg.as(RootCfg.class);
            ConnectionCfg connectionCfg = cfg.as(ConnectionCfg.class);
            DeleteModeCfg deleteModeCfg = cfg.as(DeleteModeCfg.class);
            MaxCommandJoinCountCfg maxCommandJoinCountCfg = cfg.as(MaxCommandJoinCountCfg.class);
            AbstractEntitySaveCommandImpl.ExceptionTranslatorCfg exceptionTranslatorCfg =
                    cfg.as(AbstractEntitySaveCommandImpl.ExceptionTranslatorCfg.class);
            DissociationActionCfg dissociationActionCfg = cfg.as(DissociationActionCfg.class);
            DumbBatchAcceptableCfg dumbBatchAcceptableCfg = cfg.as(DumbBatchAcceptableCfg.class);

            assert rootCfg != null;
            this.sqlClient = rootCfg.sqlClient;
            this.con = connectionCfg != null ? connectionCfg.con : null;
            this.mode = deleteModeCfg != null ? deleteModeCfg.mode : DeleteMode.AUTO;
            this.maxCommandJoinCount = maxCommandJoinCountCfg != null ?
                    maxCommandJoinCountCfg.maxCommandJoinCount :
                    sqlClient.getMaxCommandJoinCount();

            List<ExceptionTranslator<?>> exceptionTranslators = new ArrayList<>();
            exceptionTranslators.add(sqlClient.getExceptionTranslator());
            exceptionTranslators.addAll(ListNode.toList(exceptionTranslatorCfg, it -> it.listNode));
            this.exceptionTranslator = ExceptionTranslator.of(exceptionTranslators);

            this.dissociateActionMap = MapNode.toMap(dissociationActionCfg, it -> it.mapNode);
            this.dumbBatchAcceptable = dumbBatchAcceptableCfg != null && dumbBatchAcceptableCfg.acceptable;
            this.argument = (Argument) rootCfg.argument;
        }

        public OptionsImpl(
                JSqlClientImplementor sqlClient,
                Connection con
        ) {
            this(sqlClient, con, DeleteMode.PHYSICAL);
        }

        public OptionsImpl(
                JSqlClientImplementor sqlClient,
                Connection con,
                DeleteMode mode
        ) {
            this.sqlClient = sqlClient;
            this.con = con;
            this.mode = mode;
            this.maxCommandJoinCount = sqlClient.getMaxCommandJoinCount();
            this.exceptionTranslator = sqlClient.getExceptionTranslator();
            this.dissociateActionMap = Collections.emptyMap();
            this.argument = null;
        }

        @Override
        public JSqlClientImplementor getSqlClient() {
            return sqlClient;
        }

        public Argument getArgument() {
            return argument;
        }

        @Override
        public Connection getConnection() {
            return con;
        }

        @Override
        public DeleteMode getMode() {
            return mode;
        }

        @Override
        public int getMaxCommandJoinCount() {
            return maxCommandJoinCount;
        }

        public DissociateAction getDissociateAction(ImmutableProp prop) {
            DissociateAction action = dissociateActionMap.get(prop);
            if (action == null) {
                action = prop.getDissociateAction();
            }
            if (action == DissociateAction.NONE) {
                action = sqlClient.isDefaultDissociationActionCheckable() ?
                        DissociateAction.CHECK :
                        DissociateAction.LAX;
            }
            return action;
        }

        @Override
        public boolean isBatchForbidden() {
            return sqlClient.isBatchForbidden(dumbBatchAcceptable);
        }

        @Override
        public ExceptionTranslator<?> getExceptionTranslator() {
            return exceptionTranslator;
        }

        @Override
        public Triggers getTriggers() {
            return sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY ?
                    null :
                    sqlClient.getTriggers(true);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sqlClient, mode, dissociateActionMap);
        }

        @Override
        public String toString() {
            return "Data{" +
                   "sqlClient=" + sqlClient +
                   ", mode=" + mode +
                   ", dissociateActionMap=" + dissociateActionMap +
                   '}';
        }
    }

    @Override
    public DeleteCommand setMode(DeleteMode mode) {
        return new DeleteCommandImpl(new DeleteModeCfg(cfg, mode));
    }

    @Override
    public DeleteCommand setMaxCommandJoinCount(int count) {
        return new DeleteCommandImpl(new MaxCommandJoinCountCfg(cfg, count));
    }

    @Override
    public DeleteCommand setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
        return new DeleteCommandImpl(new DissociationActionCfg(cfg, prop, dissociateAction));
    }

    @Override
    public DeleteCommand setDumbBatchAcceptable(boolean acceptable) {
        return new DeleteCommandImpl(new DumbBatchAcceptableCfg(cfg, acceptable));
    }
}
