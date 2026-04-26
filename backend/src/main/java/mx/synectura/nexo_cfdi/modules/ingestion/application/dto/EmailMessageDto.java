package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EmailMessageDto(
        String messageId,
        String subject,
        String from,
        List<String> to,
        String bodyText,
        boolean hasAttachments,
        List<String> attachmentNames,
        OffsetDateTime receivedAt
) {}
