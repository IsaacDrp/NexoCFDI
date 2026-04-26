package mx.synectura.nexo_cfdi.shared.domain.user.dto;

public record RegisterUserRequest(
        String firstName,
        String paternalSurname,
        String maternalSurname,
        String rfc,
        String razonSocial,
        String postalCode
) {}
