package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.KeywordType;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeyword;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserKeywordResponse(
        UUID id,
        String phrase,
        KeywordType type,
        OffsetDateTime createdAt
) {
    public static UserKeywordResponse from(UserKeyword kw) {
        return new UserKeywordResponse(kw.id(), kw.phrase(), kw.type(), kw.createdAt());
    }
}
