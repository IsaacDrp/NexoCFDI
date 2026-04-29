package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserKeyword(
        UUID id,
        UUID userId,
        String phrase,
        KeywordType type,
        OffsetDateTime createdAt
) {}
