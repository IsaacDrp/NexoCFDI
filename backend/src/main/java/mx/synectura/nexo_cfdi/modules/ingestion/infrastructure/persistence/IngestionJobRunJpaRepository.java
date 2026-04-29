package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRunJpaRepository extends JpaRepository<IngestionJobRunEntity, UUID> {

    Optional<IngestionJobRunEntity> findFirstByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatusOrderByStartedAtDesc(
            UUID userId, int year, int month, JobStatus status);

    boolean existsByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatus(
            UUID userId, int year, int month, JobStatus status);

    List<IngestionJobRunEntity> findByTriggeredByUserIdAndTargetYearAndTargetMonth(
            UUID userId, int year, int month);

    @Modifying
    @Query("DELETE FROM IngestionJobRunEntity e WHERE e.triggeredByUser.id = :userId " +
            "AND e.targetYear = :year AND e.targetMonth = :month")
    void deleteByUserIdAndYearAndMonth(@Param("userId") UUID userId,
                                       @Param("year") int year,
                                       @Param("month") int month);
}
