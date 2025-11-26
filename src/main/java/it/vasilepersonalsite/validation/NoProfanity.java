package it.vasilepersonalsite.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = NoProfanityValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface NoProfanity {

    String message() default "Il testo inserito contiene termini non appropriati.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
