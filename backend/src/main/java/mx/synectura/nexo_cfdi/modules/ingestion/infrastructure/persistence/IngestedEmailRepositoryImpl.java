package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.EmailProcessingStatus;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedAttachment;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmail;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmailRepository;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmailSource;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class IngestedEmailRepositoryImpl implements IngestedEmailRepository {

    private final IngestedEmailJpaRepository jpa;
    private final UserJpaRepository userJpa;
    private final MailAccountJpaRepository mailAccountJpa;
    private final IngestionJobRunJpaRepository jobRunJpa;

    @Override
    @Transactional
    public IngestedEmail save(IngestedEmail email) {
        IngestedEmailEntity entity = new IngestedEmailEntity();
        entity.setUser(userJpa.getReferenceById(email.userId()));
        if (email.mailAccountId() != null) entity.setMailAccount(mailAccountJpa.getReferenceById(email.mailAccountId()));
        if (email.jobRunId() != null) entity.setJobRun(jobRunJpa.getReferenceById(email.jobRunId()));
        entity.setMessageId(email.messageId());
        entity.setSubject(email.subject());
        entity.setFromAddress(email.fromAddress());
        entity.setReceivedAt(email.receivedAt());
        entity.setHasZip(email.hasZip());
        entity.setHasXml(email.hasXml());
        entity.setHasPdf(email.hasPdf());
        entity.setMatchReasons(email.matchReasons());
        entity.setProcessingStatus(
                email.processingStatus() != null ? email.processingStatus() : EmailProcessingStatus.PENDING);
        entity.setErrorCause(email.errorCause());
        entity.setCfdiUuid(email.cfdiUuid());
        entity.setCfdiRfcEmisor(email.cfdiRfcEmisor());
        entity.setCfdiNombreEmisor(email.cfdiNombreEmisor());
        entity.setCfdiFecha(email.cfdiFecha());
        entity.setCfdiSubtotal(email.cfdiSubtotal());
        entity.setCfdiIva(email.cfdiIva());
        entity.setCfdiTotal(email.cfdiTotal());
        entity.setSource(email.source() != null ? email.source() : IngestedEmailSource.EMAIL);

        for (IngestedAttachment att : email.attachments()) {
            IngestedAttachmentEntity attEntity = new IngestedAttachmentEntity();
            attEntity.setIngestedEmail(entity);
            attEntity.setFilename(att.filename());
            attEntity.setExtension(att.extension());
            attEntity.setSizeBytes(att.sizeBytes());
            attEntity.setInsideZip(att.insideZip());
            attEntity.setParentZipName(att.parentZipName());
            attEntity.setDepth((short) att.depth());
            attEntity.setStorageKey(att.storageKey());
            entity.getAttachments().add(attEntity);
        }

        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<IngestedEmail> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public IngestedEmail update(IngestedEmail email) {
        IngestedEmailEntity entity = jpa.findById(email.id())
                .orElseThrow(() -> new IllegalArgumentException("IngestedEmail not found: " + email.id()));
        entity.setCfdiUuid(email.cfdiUuid());
        entity.setCfdiRfcEmisor(email.cfdiRfcEmisor());
        entity.setCfdiNombreEmisor(email.cfdiNombreEmisor());
        entity.setCfdiFecha(email.cfdiFecha());
        entity.setCfdiSubtotal(email.cfdiSubtotal());
        entity.setCfdiIva(email.cfdiIva());
        entity.setCfdiTotal(email.cfdiTotal());
        entity.setProcessingStatus(email.processingStatus());
        entity.setErrorCause(email.errorCause());
        entity.setHasPdf(email.hasPdf());
        entity.setHasXml(email.hasXml());
        // Solo agrega attachments nuevos (sin id asignado)
        for (IngestedAttachment att : email.attachments()) {
            if (att.id() == null) {
                IngestedAttachmentEntity attEntity = new IngestedAttachmentEntity();
                attEntity.setIngestedEmail(entity);
                attEntity.setFilename(att.filename());
                attEntity.setExtension(att.extension());
                attEntity.setSizeBytes(att.sizeBytes());
                attEntity.setInsideZip(att.insideZip());
                attEntity.setParentZipName(att.parentZipName());
                attEntity.setDepth((short) att.depth());
                attEntity.setStorageKey(att.storageKey());
                entity.getAttachments().add(attEntity);
            }
        }
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<IngestedEmail> findByUserAndReceivedBetween(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        return jpa.findByUserAndReceivedBetween(userId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findStorageKeysByUserAndPeriod(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        return jpa.findStorageKeysByUserAndPeriod(userId, from, to);
    }

    @Override
    public boolean existsByMailAccountAndMessageId(UUID mailAccountId, String messageId) {
        return jpa.existsByMailAccountIdAndMessageId(mailAccountId, messageId);
    }

    private IngestedEmail toDomain(IngestedEmailEntity e) {
        List<IngestedAttachment> atts = e.getAttachments().stream()
                .map(a -> new IngestedAttachment(
                        a.getId(), e.getId(), a.getFilename(), a.getExtension(),
                        a.getSizeBytes(), a.isInsideZip(), a.getParentZipName(),
                        a.getDepth(), a.getCreatedAt(), a.getStorageKey()))
                .collect(Collectors.toList());
        return new IngestedEmail(
                e.getId(),
                e.getUser().getId(),
                e.getMailAccount() != null ? e.getMailAccount().getId() : null,
                e.getJobRun() != null ? e.getJobRun().getId() : null,
                e.getMessageId(),
                e.getSubject(),
                e.getFromAddress(),
                e.getReceivedAt(),
                e.isHasZip(),
                e.isHasXml(),
                e.isHasPdf(),
                e.getMatchReasonsAsSet(),
                atts,
                e.getCreatedAt(),
                e.getProcessingStatus(),
                e.getErrorCause(),
                e.getCfdiUuid(),
                e.getCfdiRfcEmisor(),
                e.getCfdiNombreEmisor(),
                e.getCfdiFecha(),
                e.getCfdiSubtotal(),
                e.getCfdiIva(),
                e.getCfdiTotal(),
                e.getSource() != null ? e.getSource() : IngestedEmailSource.EMAIL);
    }
}
