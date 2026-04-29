package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record KnownInvoicer(
        UUID id,
        UUID userId,
        String emailOrDomain,
        String label,
        OffsetDateTime createdAt
) {}
