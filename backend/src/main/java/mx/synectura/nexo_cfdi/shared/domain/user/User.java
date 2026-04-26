package mx.synectura.nexo_cfdi.shared.domain.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String microsoftSub,
        String email,
        String firstName,
        String paternalSurname,
        String maternalSurname,
        String rfc,
        String razonSocial,
        String postalCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
