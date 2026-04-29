package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KeywordType;

public record CreateKeywordRequest(
        @NotBlank @Size(max = 255) String phrase,
        @NotNull KeywordType type
) {}
