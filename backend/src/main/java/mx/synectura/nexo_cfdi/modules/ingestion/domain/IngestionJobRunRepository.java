package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRunRepository {
    IngestionJobRun create(JobTrigger trigger, UUID userId, int year, int month);
    Optional<IngestionJobRun> findById(UUID id);
    Optional<IngestionJobRun> findLatestSuccess(UUID userId, int year, int month);
    boolean existsRunning(UUID userId, int year, int month);
    List<IngestionJobRun> findPreviousRuns(UUID userId, int year, int month);
    void deleteAllByUserAndPeriod(UUID userId, int year, int month);
    void markSuccess(UUID id, int accountsTotal, int accountsOk, int accountsFailed, int emailsIngested);
    void markFailed(UUID id, int accountsTotal, int accountsOk, int accountsFailed, String errorMessage);
}
