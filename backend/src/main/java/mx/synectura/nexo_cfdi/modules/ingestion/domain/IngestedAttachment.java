package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngestedAttachment(
        UUID id,
        UUID ingestedEmailId,
        String filename,
        String extension,
        long sizeBytes,
        boolean insideZip,
        String parentZipName,
        int depth,
        OffsetDateTime createdAt
) {}
