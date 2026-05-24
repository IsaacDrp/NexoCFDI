package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.application.IngestionService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.ManualCfdiRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UpdateIngestedEmailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.*;
import mx.synectura.nexo_cfdi.modules.storage.api.DocumentStoragePort;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import mx.synectura.nexo_cfdi.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private static final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");
    // Acepta tanto "2024-01-15T12:30" (datetime-local HTML) como "2024-01-15T12:30:00"
    private static final DateTimeFormatter FECHA_PARSER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();

    private final UserRepository userRepository;
    private final IngestedEmailRepository ingestedEmailRepository;
    private final IngestionJobRunRepository jobRunRepository;
    private final DocumentStoragePort documentStorage;
    private final IngestionAsyncRunner asyncRunner;

    @Override
    public JobRunResponse triggerIngestion(String microsoftSub, int year, int month, JobTrigger trigger) {
        validatePeriod(year, month);
        User user = resolveUser(microsoftSub);
        UUID userId = user.id();

        log.info("TRIGGER job_trigger={} user={} rfc={} periodo={}/{}", trigger, userId, user.rfc(), month, year);

        // Bloquea ejecuciones concurrentes para mismo período.
        if (jobRunRepository.existsRunning(userId, year, month)) {
            log.warn("YA_CORRIENDO user={} {}/{}", userId, month, year);
            throw new IngestionAlreadyRunningException(userId, year, month);
        }

        // Limpieza: eliminar datos previos del período (MinIO + BD en cascada).
        // IMPORTANTE: siempre se limpia, aunque no haya storage_keys, para evitar que
        // ingested_emails de runs anteriores (PENDING/ERROR sin upload) queden como "duplicados"
        // y el nuevo pipeline los saltee.
        OffsetDateTime[] range = monthRange(year, month);
        List<String> storageKeys = ingestedEmailRepository.findStorageKeysByUserAndPeriod(userId, range[0], range[1]);
        if (!storageKeys.isEmpty()) {
            log.info("LIMPIEZA_MINIO_INICIO user={} {}/{} objetos_minio={}", userId, month, year, storageKeys.size());
            for (String key : storageKeys) {
                try {
                    documentStorage.remove(key);
                } catch (Exception ex) {
                    log.warn("LIMPIEZA_MINIO_FAIL key={} causa={}", key, ex.getMessage());
                }
            }
        }
        boolean hadPreviousRuns = !jobRunRepository.findPreviousRuns(userId, year, month).isEmpty();
        if (hadPreviousRuns) {
            jobRunRepository.deleteAllByUserAndPeriod(userId, year, month);
            log.info("LIMPIEZA_FIN user={} {}/{} minio_keys={}", userId, month, year, storageKeys.size());
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
    public java.util.Optional<JobRunResponse> getLatestSuccessfulJob(String microsoftSub, int year, int month) {
        User user = resolveUser(microsoftSub);
        return jobRunRepository.findLatestSuccess(user.id(), year, month).map(JobRunResponse::from);
    }

    @Override
    public List<IngestedEmailResponse> findIngestedEmails(String microsoftSub, int year, int month) {
        validatePeriodNotFuture(year, month);
        User user = resolveUser(microsoftSub);
        OffsetDateTime[] range = monthRange(year, month);
        return ingestedEmailRepository.findByUserAndReceivedBetween(user.id(), range[0], range[1])
                .stream().map(IngestedEmailResponse::from).collect(Collectors.toList());
    }

    @Override
    public IngestedEmailResponse addManualEntry(String microsoftSub, ManualCfdiRequest req, MultipartFile file) {
        User user = resolveUser(microsoftSub);
        String userRfc = user.rfc() != null ? user.rfc().toUpperCase(Locale.ROOT).trim() : "SIN_RFC";

        LocalDateTime fecha;
        try {
            fecha = LocalDateTime.parse(req.getFecha(), FECHA_PARSER);
        } catch (Exception e) {
            fecha = LocalDateTime.now(MEXICO_CITY);
        }
        int year  = fecha.getYear();
        int month = fecha.getMonthValue();
        OffsetDateTime receivedAt = fecha.atZone(MEXICO_CITY).toOffsetDateTime();

        String rfcEmisor = req.getRfcEmisor() != null
                ? req.getRfcEmisor().toUpperCase(Locale.ROOT).trim() : null;

        List<IngestedAttachment> attachments = new ArrayList<>();
        boolean hasPdf = false;
        boolean hasZip = false;

        if (file != null && !file.isEmpty()) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo";
            String ext = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
            String safeId = req.getCfdiUuid() != null
                    ? req.getCfdiUuid().replaceAll("[^A-Za-z0-9._-]", "_")
                    : "m_" + System.currentTimeMillis();
            String key = String.format("%d/%02d/%s/manual/%s/%s",
                    year, month, userRfc, safeId, filename.replaceAll("[^A-Za-z0-9._-]", "_"));
            try {
                String ct = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                documentStorage.store(key, file.getBytes(), ct);
            } catch (IOException e) {
                throw new RuntimeException("Error leyendo el archivo adjunto", e);
            }
            hasPdf = "pdf".equalsIgnoreCase(ext);
            hasZip = "zip".equalsIgnoreCase(ext);
            attachments.add(new IngestedAttachment(null, null, filename, ext, file.getSize(),
                    false, null, 0, null, key));
            log.info("MANUAL_FILE_STORED user={} key={}", user.id(), key);
        }

        IngestedEmail email = new IngestedEmail(
                null, user.id(), null, null,
                null, null, null, receivedAt,
                hasZip, false, hasPdf,
                Set.of(), attachments, null,
                EmailProcessingStatus.STORED, null,
                req.getCfdiUuid(), rfcEmisor, req.getNombreEmisor(),
                fecha, req.getSubtotal(), req.getIva(), req.getTotal(),
                IngestedEmailSource.MANUAL);

        IngestedEmailResponse saved = IngestedEmailResponse.from(ingestedEmailRepository.save(email));
        log.info("MANUAL_SAVED user={} id={} rfcEmisor={} uuid={}", user.id(), saved.id(), rfcEmisor, req.getCfdiUuid());
        return saved;
    }

    @Override
    public IngestedEmailResponse updateEmail(String microsoftSub, UUID emailId,
                                             UpdateIngestedEmailRequest req, MultipartFile file) {
        User user = resolveUser(microsoftSub);
        String userRfc = user.rfc() != null ? user.rfc().toUpperCase(Locale.ROOT).trim() : "SIN_RFC";

        IngestedEmail existing = ingestedEmailRepository.findById(emailId)
                .orElseThrow(() -> new NotFoundException("IngestedEmail", emailId));
        if (!existing.userId().equals(user.id())) {
            throw new NotFoundException("IngestedEmail", emailId);
        }

        // Parsear fecha si viene
        LocalDateTime fecha = existing.cfdiFecha();
        if (req.getFecha() != null && !req.getFecha().isBlank()) {
            try { fecha = LocalDateTime.parse(req.getFecha(), FECHA_PARSER); } catch (Exception ignored) {}
        }

        String rfcEmisor = req.getRfcEmisor() != null
                ? req.getRfcEmisor().toUpperCase(Locale.ROOT).trim() : existing.cfdiRfcEmisor();
        String nombreEmisor = req.getNombreEmisor() != null ? req.getNombreEmisor() : existing.cfdiNombreEmisor();
        String cfdiUuid = req.getCfdiUuid() != null ? req.getCfdiUuid().trim() : existing.cfdiUuid();

        // Subir archivo si se provee
        List<IngestedAttachment> newAttachments = new ArrayList<>();
        boolean hasPdf = existing.hasPdf();
        boolean hasXml = existing.hasXml();

        if (file != null && !file.isEmpty()) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo";
            String ext = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
            String safeId = cfdiUuid != null ? cfdiUuid.replaceAll("[^A-Za-z0-9._-]", "_")
                    : "e_" + emailId.toString().replace("-", "");
            int year  = fecha != null ? fecha.getYear()  : LocalDateTime.now(MEXICO_CITY).getYear();
            int month = fecha != null ? fecha.getMonthValue() : LocalDateTime.now(MEXICO_CITY).getMonthValue();
            String key = String.format("%d/%02d/%s/edit/%s/%s",
                    year, month, userRfc, safeId, filename.replaceAll("[^A-Za-z0-9._-]", "_"));
            try {
                String ct = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                documentStorage.store(key, file.getBytes(), ct);
            } catch (IOException e) {
                throw new RuntimeException("Error leyendo el archivo adjunto", e);
            }
            if ("pdf".equalsIgnoreCase(ext)) hasPdf = true;
            if ("xml".equalsIgnoreCase(ext)) hasXml = true;
            newAttachments.add(new IngestedAttachment(null, emailId, filename, ext, file.getSize(),
                    false, null, 0, null, key));
            log.info("UPDATE_FILE_STORED user={} emailId={} key={}", user.id(), emailId, key);
        }

        IngestedEmail updated = new IngestedEmail(
                existing.id(), existing.userId(), existing.mailAccountId(), existing.jobRunId(),
                existing.messageId(), existing.subject(), existing.fromAddress(), existing.receivedAt(),
                existing.hasZip(), hasXml, hasPdf,
                existing.matchReasons(), newAttachments, existing.createdAt(),
                EmailProcessingStatus.STORED, null,
                cfdiUuid, rfcEmisor, nombreEmisor, fecha,
                req.getSubtotal() != null ? req.getSubtotal() : existing.cfdiSubtotal(),
                req.getIva()      != null ? req.getIva()      : existing.cfdiIva(),
                req.getTotal()    != null ? req.getTotal()    : existing.cfdiTotal(),
                existing.source());

        IngestedEmailResponse result = IngestedEmailResponse.from(ingestedEmailRepository.update(updated));
        log.info("UPDATE_SAVED user={} emailId={} rfcEmisor={} uuid={}", user.id(), emailId, rfcEmisor, cfdiUuid);
        return result;
    }

    @Override
    public String getAttachmentPreviewUrl(String microsoftSub, UUID emailId, UUID attachmentId) {
        User user = resolveUser(microsoftSub);
        IngestedEmail email = ingestedEmailRepository.findById(emailId)
                .orElseThrow(() -> new NotFoundException("IngestedEmail", emailId));
        if (!email.userId().equals(user.id())) {
            throw new NotFoundException("IngestedEmail", emailId);
        }

        IngestedAttachment target = email.attachments().stream()
                .filter(a -> a.id().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("IngestedAttachment", attachmentId));

        if (target.storageKey() == null) {
            throw new IllegalStateException("El adjunto no fue almacenado en blob store");
        }

        return documentStorage.getPresignedUrl(target.storageKey());
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
