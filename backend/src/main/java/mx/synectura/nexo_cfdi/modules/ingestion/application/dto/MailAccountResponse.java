package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailAccount;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailProvider;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.SyncStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MailAccountResponse(
        UUID id,
        String displayName,
        String emailAddress,
        MailProvider provider,
        SyncStatus status,
        OffsetDateTime lastSyncAt,
        OffsetDateTime createdAt
) {
    public static MailAccountResponse from(MailAccount account) {
        return new MailAccountResponse(
                account.id(),
                account.displayName(),
                account.emailAddress(),
                account.provider(),
                account.status(),
                account.lastSyncAt(),
                account.createdAt()
        );
    }
}
