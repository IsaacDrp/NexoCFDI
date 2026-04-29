package mx.synectura.nexo_cfdi.shared.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import mx.synectura.nexo_cfdi.shared.domain.user.PersonType;
import mx.synectura.nexo_cfdi.shared.domain.user.RegimenFiscal;
import mx.synectura.nexo_cfdi.shared.domain.user.validation.ValidRegimenFiscal;

@ValidRegimenFiscal
public record RegisterUserRequest(
        String firstName,
        String paternalSurname,
        String maternalSurname,
        String rfc,
        String razonSocial,
        String postalCode,
        @NotNull PersonType personType,
        @NotNull RegimenFiscal regimenFiscal
) {}
