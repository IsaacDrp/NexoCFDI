package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.EmailProcessingStatus;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmail;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmailSource;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MatchReason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record IngestedEmailResponse(
        UUID id,
        UUID mailAccountId,
        String messageId,
        String subject,
        String fromAddress,
        OffsetDateTime receivedAt,
        boolean hasZip,
        boolean hasXml,
        boolean hasPdf,
        Set<MatchReason> matchReasons,
        List<AttachmentInfo> attachments,
        EmailProcessingStatus processingStatus,
        String errorCause,
        String cfdiUuid,
        String cfdiRfcEmisor,
        String cfdiNombreEmisor,
        String cfdiFecha,
        BigDecimal cfdiSubtotal,
        BigDecimal cfdiIva,
        BigDecimal cfdiTotal,
        IngestedEmailSource source
) {
    public record AttachmentInfo(
            UUID id,
            String filename,
            String extension,
            long sizeBytes,
            boolean insideZip,
            String parentZipName,
            int depth,
            String storageKey
    ) {}

    public static IngestedEmailResponse from(IngestedEmail e) {
        List<AttachmentInfo> atts = e.attachments().stream()
                .map(a -> new AttachmentInfo(a.id(), a.filename(), a.extension(), a.sizeBytes(),
                        a.insideZip(), a.parentZipName(), a.depth(), a.storageKey()))
                .toList();
        return new IngestedEmailResponse(
                e.id(), e.mailAccountId(), e.messageId(), e.subject(),
                e.fromAddress(), e.receivedAt(), e.hasZip(), e.hasXml(), e.hasPdf(),
                e.matchReasons(), atts,
                e.processingStatus(), e.errorCause(), e.cfdiUuid(),
                e.cfdiRfcEmisor(),
                e.cfdiNombreEmisor(),
                e.cfdiFecha() != null ? e.cfdiFecha().toString() : null,
                e.cfdiSubtotal(),
                e.cfdiIva(),
                e.cfdiTotal(),
                e.source() != null ? e.source() : IngestedEmailSource.EMAIL);
    }
}
