package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.api.EmailReaderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.api.RawEmailMessage;
import mx.synectura.nexo_cfdi.modules.ingestion.application.detection.InvoiceDetectionService;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.*;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeywordRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Encapsula la ejecución pesada del job. Se separa del IngestionService para que el
 * proxy de Spring aplique correctamente @Async + @Transactional. Si la transacción
 * falla, hace rollback total y registra FAILED en el job_run en una transacción nueva.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionAsyncRunner {

    private final MailAccountRepository mailAccountRepository;
    private final IngestedEmailRepository ingestedEmailRepository;
    private final IngestionJobRunRepository jobRunRepository;
    private final KnownInvoicerRepository knownInvoicerRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final EmailReaderPort emailReader;
    private final InvoiceDetectionService detectionService;

    @Async("ingestionTaskExecutor")
    public void run(UUID jobRunId, UUID userId, int year, int month) {
        try {
            ProcessOutcome outcome = process(jobRunId, userId, year, month);
            jobRunRepository.markSuccess(jobRunId, outcome.accountsTotal,
                    outcome.accountsOk, outcome.accountsFailed, outcome.emailsIngested);
            log.info("Ingestión OK job={} user={} {}/{} cuentas={} correos={}",
                    jobRunId, userId, month, year, outcome.accountsTotal, outcome.emailsIngested);
        } catch (Exception ex) {
            log.error("Ingestión FALLIDA job={} user={} {}/{}", jobRunId, userId, month, year, ex);
            jobRunRepository.markFailed(jobRunId, 0, 0, 0, ex.getMessage());
        }
    }

    /**
     * Procesamiento principal. @Transactional sin rollbackFor porque RuntimeException
     * ya activa rollback por defecto. Si CUALQUIER cuenta falla, rollback total.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessOutcome process(UUID jobRunId, UUID userId, int year, int month) {
        OffsetDateTime[] range = IngestionServiceImpl.monthRange(year, month);
        OffsetDateTime from = range[0];
        OffsetDateTime to = range[1];

        List<MailAccount> accounts = mailAccountRepository.findAllByUserId(userId).stream()
                .filter(a -> a.status() == SyncStatus.ACTIVE).toList();
        List<KnownInvoicer> known = knownInvoicerRepository.findAllByUserId(userId);
        List<UserKeyword> userKeywords = userKeywordRepository.findAllByUserId(userId);

        int accountsOk = 0;
        int emailsIngested = 0;

        for (MailAccount account : accounts) {
            List<RawEmailMessage> messages = emailReader.searchInbox(account.id(), from, to);
            for (RawEmailMessage msg : messages) {
                if (ingestedEmailRepository.existsByMailAccountAndMessageId(account.id(), msg.messageId())) {
                    continue; // dedup intra-job
                }
                InvoiceDetectionService.DetectionResult det = detectionService.evaluate(msg, known, userKeywords);
                if (!det.isInvoice()) continue;

                ingestedEmailRepository.save(buildIngestedEmail(userId, account.id(), jobRunId, msg, det));
                emailsIngested++;
            }
            accountsOk++;
        }

        return new ProcessOutcome(accounts.size(), accountsOk, 0, emailsIngested);
    }

    private IngestedEmail buildIngestedEmail(UUID userId, UUID mailAccountId, UUID jobRunId,
                                             RawEmailMessage msg,
                                             InvoiceDetectionService.DetectionResult det) {
        return new IngestedEmail(
                null, userId, mailAccountId, jobRunId,
                msg.messageId(), msg.subject(), msg.fromAddress(), msg.receivedAt(),
                det.hasZip(), det.hasXml(), det.hasPdf(),
                det.reasons(),
                det.attachments(),
                null
        );
    }

    public record ProcessOutcome(int accountsTotal, int accountsOk, int accountsFailed, int emailsIngested) {}
}
