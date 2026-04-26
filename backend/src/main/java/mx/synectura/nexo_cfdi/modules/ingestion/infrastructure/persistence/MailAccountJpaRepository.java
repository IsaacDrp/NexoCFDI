package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailAccountJpaRepository extends JpaRepository<MailAccountEntity, UUID> {
    List<MailAccountEntity> findAllByUserId(UUID userId);
    Optional<MailAccountEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE MailAccountEntity m SET m.status = :status, m.syncErrorMessage = :error WHERE m.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") SyncStatus status, @Param("error") String error);

    @Modifying
    @Query("UPDATE MailAccountEntity m SET m.lastSyncAt = :now WHERE m.id = :id")
    void updateLastSyncAt(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
