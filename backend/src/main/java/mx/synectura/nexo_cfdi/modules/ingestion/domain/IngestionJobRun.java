package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngestionJobRun(
        UUID id,
        JobTrigger triggeredBy,
        UUID triggeredByUserId,
        int targetYear,
        int targetMonth,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        JobStatus status,
        int accountsTotal,
        int accountsOk,
        int accountsFailed,
        int emailsIngested,
        String errorMessage
) {}
