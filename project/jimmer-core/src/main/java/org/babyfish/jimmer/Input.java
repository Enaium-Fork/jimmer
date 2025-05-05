package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for generated input DTO class of entity type
 */
@ApiIgnore
public interface Input<E> extends View<E> {

    E toEntity();

    /**
     * For complex form contains UI tab.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    static <E> E toMergedEntity(Input<E> ... inputs) {
        E[] entities = (E[])new Object[inputs.length];
        for (int i = inputs.length - 1; i >= 0; --i) {
            entities[i] = inputs[i] != null ? inputs[i].toEntity() : null;
        }
        return ImmutableObjects.merge(entities);
    }

    static RuntimeException unknownNonNullProperty(Class<?> type, String prop) {
        return new IllegalStateException(
                "An object whose type is \"" +
                        type.getName() +
                        "\" cannot be deserialized by Jackson. " +
                        "the property \"" +
                        prop +
                        "\" must be specified it is non-null property"
        );
    }

    static RuntimeException unknownNullableProperty(Class<?> type, String prop) {
        return new IllegalStateException(
                "An object whose type is \"" +
                        type.getName() +
                        "\" cannot be deserialized by Jackson. " +
                        "The current input has the fixed nullable property \"" +
                        prop +
                        "\", it is not specified by JSON explicitly. " +
                        "Please either explicitly specify the property as null in the JSON, " +
                        "or specify the current input property as static, dynamic or fuzzy in the DTO language"
        );
    }
}
