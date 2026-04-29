package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailAccountRepository {
    List<MailAccount> findAllByUserId(UUID userId);
    Optional<MailAccount> findByIdAndUserId(UUID id, UUID userId);
    MailAccount save(MailAccount account, String encryptedRefreshToken, String tokenIv);
    void updateStatus(UUID id, SyncStatus status, String errorMessage);
    void updateLastSyncAt(UUID id);

    /** Devuelve user_ids únicos que tienen al menos una cuenta en el estado dado. */
    List<UUID> findDistinctUserIdsByStatus(SyncStatus status);
}
