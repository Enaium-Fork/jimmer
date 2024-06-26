package org.babyfish.jimmer.internal;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * There is no substantial functionality, just to alert the user
 *
 * <p>The split of `@javax.annotation.Generated` and
 * `@jakatra.annotation.Generated` brings great trouble,
 * simply customize one.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION})
public @interface GeneratedBy {

    Class<?> type() default void.class;

    String file() default "";

    String prompt() default "";
}
