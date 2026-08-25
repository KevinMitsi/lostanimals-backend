package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defense-in-depth validation for inbound models. Database access must still use
 * bound parameters; input validation is not a replacement for parameterized SQL.
 */
@Documented
@Constraint(validatedBy = NoSqlInjectionValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSqlInjection {

    String message() default "must not contain SQL syntax";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
