package mx.synectura.nexo_cfdi.modules.ingestion.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mensaje crudo leído del buzón. Los adjuntos viven en memoria como byte[].
 * Pensado para vivir solo durante la transacción de ingestión.
 */
public record RawEmailMessage(
        String messageId,
        String subject,
        String fromAddress,
        OffsetDateTime receivedAt,
        String bodyText,
        List<RawAttachment> attachments
) {
    public record RawAttachment(
            String filename,
            byte[] content
    ) {}
}
