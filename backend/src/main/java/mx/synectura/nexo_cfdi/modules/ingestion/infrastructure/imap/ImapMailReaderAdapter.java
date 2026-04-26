package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.imap;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.api.EmailReaderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.EmailMessageDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Implementación IMAP con autenticación XOAUTH2.
 * TODO: implementar conexión real usando jakarta.mail con SASL XOAUTH2.
 */
@Component
@RequiredArgsConstructor
public class ImapMailReaderAdapter implements EmailReaderPort {

    @Override
    public List<EmailMessageDto> readInbox(UUID mailAccountId, int maxMessages) {
        // Stub — implementación completa en Épica 1
        return List.of();
    }
}
