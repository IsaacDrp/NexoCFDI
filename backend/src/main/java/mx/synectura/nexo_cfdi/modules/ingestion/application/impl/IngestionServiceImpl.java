package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.application.IngestionService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.*;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import mx.synectura.nexo_cfdi.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private static final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");

    private final UserRepository userRepository;
    private final IngestedEmailRepository ingestedEmailRepository;
    private final IngestionJobRunRepository jobRunRepository;
    private final IngestionAsyncRunner asyncRunner;

    @Override
    public JobRunResponse triggerIngestion(String microsoftSub, int year, int month, JobTrigger trigger) {
        validatePeriod(year, month);
        User user = resolveUser(microsoftSub);
        UUID userId = user.id();

        log.info("TRIGGER job_trigger={} user={} rfc={} periodo={}/{}", trigger, userId, user.rfc(), month, year);

        // Idempotencia: si ya hay SUCCESS para ese período, devolver el último.
        var existingSuccess = jobRunRepository.findLatestSuccess(userId, year, month);
        if (existingSuccess.isPresent()) {
            log.info("IDEMPOTENTE: ya existe SUCCESS job={} user={} {}/{}", existingSuccess.get().id(), userId, month, year);
            return JobRunResponse.from(existingSuccess.get());
        }

        // Bloquea ejecuciones concurrentes para mismo período.
        if (jobRunRepository.existsRunning(userId, year, month)) {
            log.warn("YA_CORRIENDO user={} {}/{}", userId, month, year);
            throw new IngestionAlreadyRunningException(userId, year, month);
        }

        IngestionJobRun jobRun = jobRunRepository.create(trigger, userId, year, month);
        log.info("JOB_CREADO job={} user={} {}/{} trigger={}", jobRun.id(), userId, month, year, trigger);
        asyncRunner.run(jobRun.id(), userId, year, month);
        return JobRunResponse.from(jobRun);
    }

    @Override
    public JobRunResponse getJobStatus(UUID jobRunId) {
        return jobRunRepository.findById(jobRunId)
                .map(JobRunResponse::from)
                .orElseThrow(() -> new NotFoundException("IngestionJobRun", jobRunId));
    }

    @Override
    public List<IngestedEmailResponse> findIngestedEmails(String microsoftSub, int year, int month) {
        validatePeriodNotFuture(year, month);
        User user = resolveUser(microsoftSub);
        OffsetDateTime[] range = monthRange(year, month);
        return ingestedEmailRepository.findByUserAndReceivedBetween(user.id(), range[0], range[1])
                .stream().map(IngestedEmailResponse::from).collect(Collectors.toList());
    }

    private User resolveUser(String microsoftSub) {
        return userRepository.findByMicrosoftSub(microsoftSub)
                .orElseThrow(() -> new NotFoundException("User", microsoftSub));
    }

    /** El mes objetivo debe ser pasado, NUNCA el mes en curso ni futuro. */
    private void validatePeriod(int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        YearMonth current = YearMonth.now(MEXICO_CITY);
        if (!target.isBefore(current)) {
            throw new InvalidIngestionPeriodException(
                    "Solo se permite ingerir meses cerrados. Solicitado: " + target + ", actual: " + current);
        }
    }

    private void validatePeriodNotFuture(int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        YearMonth current = YearMonth.now(MEXICO_CITY);
        if (target.isAfter(current)) {
            throw new InvalidIngestionPeriodException("Mes futuro no permitido: " + target);
        }
    }

    static OffsetDateTime[] monthRange(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        OffsetDateTime from = ym.atDay(1).atStartOfDay(MEXICO_CITY).toOffsetDateTime();
        OffsetDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay(MEXICO_CITY).toOffsetDateTime();
        return new OffsetDateTime[]{from, to};
    }
}
