package org.babyfish.jimmer.sql.exception;

import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;
import org.babyfish.jimmer.sql.runtime.ExportedSavePath;

@ErrorFamily
@ErrorField(name = "exportedPath", type = ExportedSavePath.class)
public enum SaveErrorCode {

    READONLY_MIDDLE_TABLE,

    NULL_TARGET,

    CANNOT_DISSOCIATE_TARGETS,

    NO_ID_GENERATOR,

    ILLEGAL_ID_GENERATOR,

    ILLEGAL_GENERATED_ID,

    ILLEGAL_INTERCEPTOR_BEHAVIOR,

    EMPTY_OBJECT,

    NO_KEY_PROPS,

    NO_KEY_PROP,

    NO_NON_ID_PROPS,

    NO_VERSION,

    OPTIMISTIC_LOCK_ERROR,

    /**
     * Only case when
     * 1. The transaction in trigger is enabled
     * 2. Save mode is `INSERT_ONLY` or associated mode is `APPEND`
     */
    ALREADY_EXISTS,

    NEITHER_ID_NOR_KEY,

    REVERSED_REMOTE_ASSOCIATION,

    LONG_REMOTE_ASSOCIATION,

    FAILED_REMOTE_VALIDATION,

    UNSTRUCTURED_ASSOCIATION,

    TARGET_IS_NOT_TRANSFERABLE,

    INCOMPLETE_PROPERTY,

    @ErrorField(name = "props", type = String.class, list = true)
    @ErrorField(name = "values", type = Object.class, list = true)
    NOT_UNIQUE,

    @ErrorField(name = "prop", type = String.class)
    @ErrorField(name = "targetId", type = Object.class)
    ILLEGAL_TARGET_ID,

    @ErrorField(name = "backReferenceProp", type = String.class)
    UNLOADED_FROZEN_BACK_REFERENCE;

    /**
     * Will be removed in `1.0.0`
     */
    @Deprecated
    public static final SaveErrorCode ILLEGAL_VERSION = OPTIMISTIC_LOCK_ERROR;
}
