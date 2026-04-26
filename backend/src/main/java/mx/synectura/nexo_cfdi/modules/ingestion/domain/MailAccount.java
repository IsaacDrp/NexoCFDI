package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MailAccount(
        UUID id,
        UUID userId,
        String displayName,
        String emailAddress,
        MailProvider provider,
        SyncStatus status,
        OffsetDateTime lastSyncAt,
        OffsetDateTime createdAt
) {}
