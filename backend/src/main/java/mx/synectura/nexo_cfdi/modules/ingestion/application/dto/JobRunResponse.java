package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestionJobRun;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobRunResponse(
        UUID id,
        String triggeredBy,
        Integer targetYear,
        Integer targetMonth,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String status,
        Integer accountsTotal,
        Integer accountsOk,
        Integer accountsFailed,
        Integer emailsIngested,
        String errorMessage
) {
    public static JobRunResponse from(IngestionJobRun r) {
        return new JobRunResponse(
                r.id(),
                r.triggeredBy().name(),
                r.targetYear(),
                r.targetMonth(),
                r.startedAt(),
                r.finishedAt(),
                r.status().name(),
                r.accountsTotal(),
                r.accountsOk(),
                r.accountsFailed(),
                r.emailsIngested(),
                r.errorMessage()
        );
    }
}
