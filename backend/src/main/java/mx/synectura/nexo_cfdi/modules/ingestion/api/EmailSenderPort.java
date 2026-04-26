package mx.synectura.nexo_cfdi.modules.ingestion.api;

import java.util.List;
import java.util.UUID;

public interface EmailSenderPort {
    void sendEmail(UUID mailAccountId, String to, String subject, String body);
    void sendEmailWithAttachment(UUID mailAccountId, String to, String subject, String body,
                                 String attachmentName, byte[] attachmentContent);
}
