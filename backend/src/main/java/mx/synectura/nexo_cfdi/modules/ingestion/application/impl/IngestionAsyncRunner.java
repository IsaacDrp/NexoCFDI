package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.api.EmailReaderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.api.RawEmailMessage;
import mx.synectura.nexo_cfdi.modules.ingestion.application.detection.InvoiceDetectionService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.detection.ZipScanner;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.*;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeywordRepository;
import mx.synectura.nexo_cfdi.modules.processor.api.CfdiParserPort;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiData;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiParseException;
import mx.synectura.nexo_cfdi.modules.storage.api.DocumentStoragePort;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionAsyncRunner {

    private final MailAccountRepository mailAccountRepository;
    private final IngestedEmailRepository ingestedEmailRepository;
    private final IngestionJobRunRepository jobRunRepository;
    private final KnownInvoicerRepository knownInvoicerRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final UserRepository userRepository;
    private final EmailReaderPort emailReader;
    private final InvoiceDetectionService detectionService;
    private final ZipScanner zipScanner;
    private final CfdiParserPort cfdiParser;
    private final DocumentStoragePort documentStorage;

    @Async("ingestionTaskExecutor")
    public void run(UUID jobRunId, UUID userId, int year, int month) {
        MDC.put("job", jobRunId.toString());
        long t0 = System.currentTimeMillis();
        log.info("PIPELINE INICIO job={} user={} periodo={}/{} hilo={}",
                jobRunId, userId, month, year, Thread.currentThread().getName());
        try {
            ProcessOutcome outcome = process(jobRunId, userId, year, month);
            long ms = System.currentTimeMillis() - t0;
            jobRunRepository.markSuccess(jobRunId, outcome.accountsTotal(),
                    outcome.accountsOk(), outcome.accountsFailed(), outcome.emailsIngested());
            log.info("PIPELINE OK job={} user={} periodo={}/{} cuentas={} facturas={} ms={}",
                    jobRunId, userId, month, year, outcome.accountsTotal(), outcome.emailsIngested(), ms);
        } catch (Exception ex) {
            long ms = System.currentTimeMillis() - t0;
            log.error("PIPELINE FALLIDO job={} user={} periodo={}/{} ms={} causa={}",
                    jobRunId, userId, month, year, ms, ex.getMessage(), ex);
            jobRunRepository.markFailed(jobRunId, 0, 0, 0, ex.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessOutcome process(UUID jobRunId, UUID userId, int year, int month) {
        OffsetDateTime[] range = IngestionServiceImpl.monthRange(year, month);
        OffsetDateTime from = range[0];
        OffsetDateTime to   = range[1];

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + userId));
        String userRfc = user.rfc() == null ? null : user.rfc().toUpperCase(Locale.ROOT).trim();

        List<MailAccount> accounts     = mailAccountRepository.findAllByUserId(userId).stream()
                .filter(a -> a.status() == SyncStatus.ACTIVE).toList();
        List<KnownInvoicer> known      = knownInvoicerRepository.findAllByUserId(userId);
        List<UserKeyword>   userKeywords = userKeywordRepository.findAllByUserId(userId);

        log.info("PROCESS job={} user={} rfc={} periodo={}/{} cuentas_activas={} known_invoicers={} keywords={}",
                jobRunId, userId, userRfc, month, year,
                accounts.size(), known.size(), userKeywords.size());

        int accountsOk     = 0;
        int emailsIngested = 0;

        for (MailAccount account : accounts) {
            log.info("CUENTA INICIO job={} cuenta={} buscando correos entre {} y {}",
                    jobRunId, account.id(), from, to);

            List<RawEmailMessage> messages = emailReader.searchInbox(account.id(), from, to);
            log.info("CUENTA ENCONTRADOS job={} cuenta={} total_correos={}",
                    jobRunId, account.id(), messages.size());

            int vistos = 0, duplicados = 0, noFactura = 0, facturas = 0;

            for (RawEmailMessage msg : messages) {
                vistos++;
                if (ingestedEmailRepository.existsByMailAccountAndMessageId(account.id(), msg.messageId())) {
                    duplicados++;
                    log.debug("DUPLICADO job={} cuenta={} msgId={}", jobRunId, account.id(), msg.messageId());
                    continue;
                }

                InvoiceDetectionService.DetectionResult det =
                        detectionService.evaluate(msg, known, userKeywords);

                if (!det.isInvoice()) {
                    noFactura++;
                    log.debug("NO_FACTURA job={} msgId={} from={} subject=\"{}\"",
                            jobRunId, msg.messageId(), msg.fromAddress(), msg.subject());
                    continue;
                }

                facturas++;
                log.info("FACTURA DETECTADA job={} msgId={} from={} subject=\"{}\" razones={} hasXml={} hasPdf={} hasZip={} adjuntos={}",
                        jobRunId, msg.messageId(), msg.fromAddress(), msg.subject(),
                        det.reasons(), det.hasXml(), det.hasPdf(), det.hasZip(), det.attachments().size());

                IngestedEmail saved = processEmail(userId, account.id(), jobRunId, year, month,
                        msg, det, userRfc);
                if (saved != null) emailsIngested++;
            }

            accountsOk++;
            log.info("CUENTA FIN job={} cuenta={} vistos={} duplicados={} no_factura={} facturas={}",
                    jobRunId, account.id(), vistos, duplicados, noFactura, facturas);
        }

        log.info("PROCESS FIN job={} user={} cuentas_total={} cuentas_ok={} emails_ingestados={}",
                jobRunId, userId, accounts.size(), accountsOk, emailsIngested);

        return new ProcessOutcome(accounts.size(), accountsOk, 0, emailsIngested);
    }

    private IngestedEmail processEmail(UUID userId, UUID mailAccountId, UUID jobRunId,
                                       int year, int month,
                                       RawEmailMessage msg,
                                       InvoiceDetectionService.DetectionResult det,
                                       String userRfc) {
        String mid = msg.messageId();

        // ── Paso 1: Verificar XML ────────────────────────────────────────────
        if (!det.hasXml()) {
            log.warn("NO_XML job={} msgId={} adjuntos={} → ERROR guardado",
                    jobRunId, mid, det.attachments().size());
            return ingestedEmailRepository.save(
                    buildEmail(userId, mailAccountId, jobRunId, msg, det,
                            EmailProcessingStatus.ERROR, "NO_XML", null, det.attachments()));
        }

        // ── Paso 2: Extraer bytes del XML ────────────────────────────────────
        Optional<FileBytes> xmlOpt = findFile(msg, "xml");
        if (xmlOpt.isEmpty()) {
            log.warn("XML_NO_EXTRAIBLE job={} msgId={} det.hasXml=true pero no se localizaron bytes → ERROR",
                    jobRunId, mid);
            return ingestedEmailRepository.save(
                    buildEmail(userId, mailAccountId, jobRunId, msg, det,
                            EmailProcessingStatus.ERROR, "NO_XML", null, det.attachments()));
        }
        FileBytes xmlFile = xmlOpt.get();
        log.debug("XML_EXTRAIDO job={} msgId={} archivo={} bytes={}",
                jobRunId, mid, xmlFile.filename(), xmlFile.content().length);

        // ── Paso 3: Parsear CFDI ─────────────────────────────────────────────
        CfdiData cfdi;
        try {
            cfdi = cfdiParser.parse(xmlFile.content());
            log.info("CFDI_PARSEADO job={} msgId={} rfcReceptor={} rfcEmisor={} uuid={}",
                    jobRunId, mid, cfdi.rfcReceptor(), cfdi.rfcEmisor(), cfdi.uuid());
        } catch (CfdiParseException ex) {
            log.warn("CFDI_PARSE_ERROR job={} msgId={} archivo={} causa={} → ERROR",
                    jobRunId, mid, xmlFile.filename(), ex.getMessage());
            return ingestedEmailRepository.save(
                    buildEmail(userId, mailAccountId, jobRunId, msg, det,
                            EmailProcessingStatus.ERROR, "PARSE_ERROR: " + ex.getMessage(),
                            null, det.attachments()));
        }

        // ── Paso 4: Validar RFC ──────────────────────────────────────────────
        String rfcEnDoc = cfdi.rfcReceptor() == null ? null
                : cfdi.rfcReceptor().toUpperCase(Locale.ROOT).trim();
        if (userRfc == null || !userRfc.equals(rfcEnDoc)) {
            log.warn("RFC_MISMATCH job={} msgId={} esperado={} encontrado={} uuid={} → ERROR",
                    jobRunId, mid, userRfc, rfcEnDoc, cfdi.uuid());
            return ingestedEmailRepository.save(
                    buildEmail(userId, mailAccountId, jobRunId, msg, det,
                            EmailProcessingStatus.ERROR,
                            "RFC_MISMATCH: esperado=" + userRfc + " encontrado=" + rfcEnDoc,
                            cfdi.uuid(), det.attachments()));
        }
        log.debug("RFC_OK job={} msgId={} rfc={}", jobRunId, mid, userRfc);

        // ── Paso 5: Construir ruta ────────────────────────────────────────────
        String safeMsg  = sanitize(msg.messageId());
        String keyBase  = String.format("%d/%02d/%s/%s", year, month, userRfc, safeMsg);
        log.debug("RUTA_BASE job={} msgId={} keyBase={}", jobRunId, mid, keyBase);

        // ── Paso 6: Subir XML ────────────────────────────────────────────────
        String xmlKey = keyBase + "/" + sanitize(xmlFile.filename());
        long t0 = System.currentTimeMillis();
        documentStorage.store(xmlKey, xmlFile.content(), "application/xml");
        log.info("XML_STORED job={} msgId={} key={} bytes={} ms={}",
                jobRunId, mid, xmlKey, xmlFile.content().length, System.currentTimeMillis() - t0);

        // ── Paso 7: Subir PDF ────────────────────────────────────────────────
        String pdfKey = null;
        Optional<FileBytes> pdfOpt = findFile(msg, "pdf");
        if (pdfOpt.isPresent()) {
            pdfKey = keyBase + "/" + sanitize(pdfOpt.get().filename());
            long t1 = System.currentTimeMillis();
            documentStorage.store(pdfKey, pdfOpt.get().content(), "application/pdf");
            log.info("PDF_STORED job={} msgId={} key={} bytes={} ms={}",
                    jobRunId, mid, pdfKey, pdfOpt.get().content().length, System.currentTimeMillis() - t1);
        } else {
            log.debug("PDF_NO_ENCONTRADO job={} msgId={}", jobRunId, mid);
        }

        // ── Paso 8: Persistir con storage keys ───────────────────────────────
        String finalXmlKey = xmlKey;
        String finalPdfKey = pdfKey;
        FileBytes pdfFile  = pdfOpt.orElse(null);

        List<IngestedAttachment> attachmentsConKeys = det.attachments().stream()
                .map(a -> {
                    if ("xml".equals(a.extension()) && a.filename().equals(xmlFile.filename())) {
                        return withKey(a, finalXmlKey);
                    }
                    if (pdfFile != null && "pdf".equals(a.extension()) && a.filename().equals(pdfFile.filename())) {
                        return withKey(a, finalPdfKey);
                    }
                    return a;
                })
                .toList();

        IngestedEmail saved = ingestedEmailRepository.save(
                buildEmail(userId, mailAccountId, jobRunId, msg, det,
                        EmailProcessingStatus.STORED, null, cfdi.uuid(), attachmentsConKeys));

        log.info("EMAIL_STORED job={} msgId={} emailId={} uuid={} xmlKey={} pdfKey={}",
                jobRunId, mid, saved.id(), cfdi.uuid(), xmlKey, pdfKey != null ? pdfKey : "-");
        return saved;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Optional<FileBytes> findFile(RawEmailMessage msg, String extension) {
        for (RawEmailMessage.RawAttachment att : msg.attachments()) {
            if (extension.equals(ZipScanner.extensionOf(att.filename()))) {
                log.debug("FILE_FOUND_ROOT ext={} archivo={}", extension, att.filename());
                return Optional.of(new FileBytes(att.filename(), att.content()));
            }
        }
        for (RawEmailMessage.RawAttachment att : msg.attachments()) {
            if (ZipScanner.isZip(att.filename())) {
                log.debug("SCANNING_ZIP archivo={} buscando .{}", att.filename(), extension);
                for (ZipScanner.ExtractedFile ef : zipScanner.extract(att.filename(), att.content())) {
                    if (extension.equals(ef.extension())) {
                        log.debug("FILE_FOUND_ZIP ext={} archivo={} zip={}", extension, ef.filename(), att.filename());
                        return Optional.of(new FileBytes(ef.filename(), ef.content()));
                    }
                }
            }
        }
        log.debug("FILE_NOT_FOUND ext={} adjuntos={}", extension, msg.attachments().size());
        return Optional.empty();
    }

    private IngestedEmail buildEmail(UUID userId, UUID mailAccountId, UUID jobRunId,
                                     RawEmailMessage msg,
                                     InvoiceDetectionService.DetectionResult det,
                                     EmailProcessingStatus status,
                                     String errorCause,
                                     String cfdiUuid,
                                     List<IngestedAttachment> attachments) {
        return new IngestedEmail(
                null, userId, mailAccountId, jobRunId,
                msg.messageId(), msg.subject(), msg.fromAddress(), msg.receivedAt(),
                det.hasZip(), det.hasXml(), det.hasPdf(),
                det.reasons(), attachments, null,
                status, errorCause, cfdiUuid);
    }

    private static IngestedAttachment withKey(IngestedAttachment a, String key) {
        return new IngestedAttachment(a.id(), a.ingestedEmailId(), a.filename(), a.extension(),
                a.sizeBytes(), a.insideZip(), a.parentZipName(), a.depth(), a.createdAt(), key);
    }

    private static String sanitize(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record FileBytes(String filename, byte[] content) {}

    public record ProcessOutcome(int accountsTotal, int accountsOk, int accountsFailed, int emailsIngested) {}
}
