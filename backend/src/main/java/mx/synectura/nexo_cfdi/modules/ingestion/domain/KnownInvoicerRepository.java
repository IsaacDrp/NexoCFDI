package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.util.List;
import java.util.UUID;

public interface KnownInvoicerRepository {
    List<KnownInvoicer> findAllByUserId(UUID userId);
}
