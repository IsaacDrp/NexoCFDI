package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRunJpaRepository extends JpaRepository<IngestionJobRunEntity, UUID> {

    Optional<IngestionJobRunEntity> findFirstByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatusOrderByStartedAtDesc(
            UUID userId, int year, int month, JobStatus status);

    boolean existsByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatus(
            UUID userId, int year, int month, JobStatus status);
}
