package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.smtp;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.api.EmailSenderPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación SMTP con autenticación XOAUTH2.
 * TODO: implementar envío real usando jakarta.mail con SASL XOAUTH2.
 */
@Component
@RequiredArgsConstructor
public class SmtpMailSenderAdapter implements EmailSenderPort {

    @Override
    public void sendEmail(UUID mailAccountId, String to, String subject, String body) {
        // Stub — implementación completa en Épica 1
    }

    @Override
    public void sendEmailWithAttachment(UUID mailAccountId, String to, String subject,
                                        String body, String attachmentName, byte[] attachmentContent) {
        // Stub — implementación completa en Épica 1
    }
}
