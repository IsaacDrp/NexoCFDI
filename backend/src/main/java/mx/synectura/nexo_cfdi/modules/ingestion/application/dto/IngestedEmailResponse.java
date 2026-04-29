package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmail;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MatchReason;

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
        List<AttachmentInfo> attachments
) {
    public record AttachmentInfo(
            String filename,
            String extension,
            long sizeBytes,
            boolean insideZip,
            String parentZipName,
            int depth
    ) {}

    public static IngestedEmailResponse from(IngestedEmail e) {
        List<AttachmentInfo> atts = e.attachments().stream()
                .map(a -> new AttachmentInfo(a.filename(), a.extension(), a.sizeBytes(),
                        a.insideZip(), a.parentZipName(), a.depth()))
                .toList();
        return new IngestedEmailResponse(e.id(), e.mailAccountId(), e.messageId(), e.subject(),
                e.fromAddress(), e.receivedAt(), e.hasZip(), e.hasXml(), e.hasPdf(),
                e.matchReasons(), atts);
    }
}
