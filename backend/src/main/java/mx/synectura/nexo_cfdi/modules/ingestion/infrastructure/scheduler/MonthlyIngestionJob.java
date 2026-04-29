package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.application.impl.IngestionAsyncRunner;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestionJobRun;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestionJobRunRepository;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailAccountRepository;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.SyncStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Job mensual: el día 01 a las 03:00 (America/Mexico_City) ingiere correos
 * del mes anterior para todos los usuarios con al menos una cuenta ACTIVE.
 * Dispara la ejecución async por usuario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyIngestionJob {

    private static final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");

    private final MailAccountRepository mailAccountRepository;
    private final IngestionJobRunRepository jobRunRepository;
    private final IngestionAsyncRunner asyncRunner;

    @Scheduled(cron = "0 0 3 1 * *", zone = "America/Mexico_City")
    public void runForPreviousMonth() {
        YearMonth previous = YearMonth.from(LocalDate.now(MEXICO_CITY)).minusMonths(1);
        int year = previous.getYear();
        int month = previous.getMonthValue();
        log.info("Disparando ingestión mensual programada para {}/{}", month, year);

        List<UUID> userIds = mailAccountRepository.findDistinctUserIdsByStatus(SyncStatus.ACTIVE);
        for (UUID userId : userIds) {
            try {
                if (jobRunRepository.findLatestSuccess(userId, year, month).isPresent()) {
                    log.info("Skip user={} ya tiene SUCCESS para {}/{}", userId, month, year);
                    continue;
                }
                if (jobRunRepository.existsRunning(userId, year, month)) {
                    log.info("Skip user={} ya tiene job RUNNING para {}/{}", userId, month, year);
                    continue;
                }
                IngestionJobRun jobRun = jobRunRepository.create(JobTrigger.SCHEDULED, userId, year, month);
                asyncRunner.run(jobRun.id(), userId, year, month);
            } catch (Exception ex) {
                log.error("Fallo encolando ingestión para user={} {}/{}", userId, month, year, ex);
            }
        }
    }
}
