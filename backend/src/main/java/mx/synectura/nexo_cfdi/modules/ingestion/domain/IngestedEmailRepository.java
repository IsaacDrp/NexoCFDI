package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IngestedEmailRepository {
    IngestedEmail save(IngestedEmail email);
    List<IngestedEmail> findByUserAndReceivedBetween(UUID userId, OffsetDateTime from, OffsetDateTime to);
    boolean existsByMailAccountAndMessageId(UUID mailAccountId, String messageId);
}
