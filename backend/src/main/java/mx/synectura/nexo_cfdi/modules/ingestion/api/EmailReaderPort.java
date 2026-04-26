package mx.synectura.nexo_cfdi.modules.ingestion.api;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.EmailMessageDto;

import java.util.List;
import java.util.UUID;

public interface EmailReaderPort {
    List<EmailMessageDto> readInbox(UUID mailAccountId, int maxMessages);
}
