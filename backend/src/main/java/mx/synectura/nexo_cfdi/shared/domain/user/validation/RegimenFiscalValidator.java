package mx.synectura.nexo_cfdi.shared.domain.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.RegisterUserRequest;

public class RegimenFiscalValidator implements ConstraintValidator<ValidRegimenFiscal, RegisterUserRequest> {

    @Override
    public boolean isValid(RegisterUserRequest req, ConstraintValidatorContext ctx) {
        if (req.personType() == null || req.regimenFiscal() == null) {
            return true; // @NotNull handles individual field nullability
        }
        return req.regimenFiscal().isValidFor(req.personType());
    }
}
