package mx.synectura.nexo_cfdi.shared.domain.user.dto;

import mx.synectura.nexo_cfdi.shared.domain.user.PersonType;
import mx.synectura.nexo_cfdi.shared.domain.user.RegimenFiscal;
import mx.synectura.nexo_cfdi.shared.domain.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String paternalSurname,
        String maternalSurname,
        String rfc,
        String razonSocial,
        String postalCode,
        PersonType personType,
        RegimenFiscal regimenFiscal,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.id(), u.email(), u.firstName(), u.paternalSurname(),
                u.maternalSurname(), u.rfc(), u.razonSocial(), u.postalCode(),
                u.personType(), u.regimenFiscal(), u.createdAt());
    }
}
