package mx.synectura.nexo_cfdi.shared.domain.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RegimenFiscalValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegimenFiscal {
    String message() default "El régimen fiscal no es válido para el tipo de persona seleccionado";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
