package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestionJobRun;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestionJobRunRepository;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobStatus;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IngestionJobRunRepositoryImpl implements IngestionJobRunRepository {

    private final IngestionJobRunJpaRepository jpa;
    private final UserJpaRepository userJpa;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionJobRun create(JobTrigger trigger, UUID userId, int year, int month) {
        IngestionJobRunEntity entity = new IngestionJobRunEntity();
        entity.setTriggeredBy(trigger);
        if (userId != null) {
            entity.setTriggeredByUser(userJpa.getReferenceById(userId));
        }
        entity.setTargetYear(year);
        entity.setTargetMonth(month);
        entity.setStatus(JobStatus.RUNNING);
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<IngestionJobRun> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<IngestionJobRun> findLatestSuccess(UUID userId, int year, int month) {
        return jpa.findFirstByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatusOrderByStartedAtDesc(
                userId, year, month, JobStatus.SUCCESS).map(this::toDomain);
    }

    @Override
    public boolean existsRunning(UUID userId, int year, int month) {
        return jpa.existsByTriggeredByUserIdAndTargetYearAndTargetMonthAndStatus(
                userId, year, month, JobStatus.RUNNING);
    }

    @Override
    public List<IngestionJobRun> findPreviousRuns(UUID userId, int year, int month) {
        return jpa.findByTriggeredByUserIdAndTargetYearAndTargetMonth(userId, year, month)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteAllByUserAndPeriod(UUID userId, int year, int month) {
        jpa.deleteByUserIdAndYearAndMonth(userId, year, month);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID id, int accountsTotal, int accountsOk, int accountsFailed, int emailsIngested) {
        IngestionJobRunEntity entity = jpa.findById(id).orElseThrow();
        entity.setStatus(JobStatus.SUCCESS);
        entity.setFinishedAt(OffsetDateTime.now());
        entity.setAccountsTotal(accountsTotal);
        entity.setAccountsOk(accountsOk);
        entity.setAccountsFailed(accountsFailed);
        entity.setEmailsIngested(emailsIngested);
        jpa.save(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, int accountsTotal, int accountsOk, int accountsFailed, String errorMessage) {
        IngestionJobRunEntity entity = jpa.findById(id).orElseThrow();
        entity.setStatus(JobStatus.FAILED);
        entity.setFinishedAt(OffsetDateTime.now());
        entity.setAccountsTotal(accountsTotal);
        entity.setAccountsOk(accountsOk);
        entity.setAccountsFailed(accountsFailed);
        entity.setEmailsIngested(0);
        entity.setErrorMessage(errorMessage);
        jpa.save(entity);
    }

    private IngestionJobRun toDomain(IngestionJobRunEntity e) {
        return new IngestionJobRun(
                e.getId(),
                e.getTriggeredBy(),
                e.getTriggeredByUser() != null ? e.getTriggeredByUser().getId() : null,
                e.getTargetYear(),
                e.getTargetMonth(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getStatus(),
                e.getAccountsTotal(),
                e.getAccountsOk(),
                e.getAccountsFailed(),
                e.getEmailsIngested(),
                e.getErrorMessage()
        );
    }
}
