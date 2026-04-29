package mx.synectura.nexo_cfdi.modules.ingestion.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EmailReaderPort {
    /**
     * Búsqueda en INBOX por rango de fechas (basado en fecha de recepción).
     * Devuelve mensajes con todos sus adjuntos cargados en memoria.
     */
    List<RawEmailMessage> searchInbox(UUID mailAccountId, OffsetDateTime from, OffsetDateTime to);
}
