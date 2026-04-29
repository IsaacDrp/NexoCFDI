package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record IngestedEmail(
        UUID id,
        UUID userId,
        UUID mailAccountId,
        UUID jobRunId,
        String messageId,
        String subject,
        String fromAddress,
        OffsetDateTime receivedAt,
        boolean hasZip,
        boolean hasXml,
        boolean hasPdf,
        Set<MatchReason> matchReasons,
        List<IngestedAttachment> attachments,
        OffsetDateTime createdAt,
        EmailProcessingStatus processingStatus,
        String errorCause,
        String cfdiUuid,
        String cfdiRfcEmisor,
        String cfdiNombreEmisor,
        LocalDateTime cfdiFecha,
        BigDecimal cfdiSubtotal,
        BigDecimal cfdiIva,
        BigDecimal cfdiTotal,
        IngestedEmailSource source
) {}
