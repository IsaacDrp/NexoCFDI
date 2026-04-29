package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KnownInvoicer;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KnownInvoicerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class KnownInvoicerRepositoryImpl implements KnownInvoicerRepository {

    private final KnownInvoicerJpaRepository jpa;

    @Override
    public List<KnownInvoicer> findAllByUserId(UUID userId) {
        return jpa.findAllByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private KnownInvoicer toDomain(KnownInvoicerEntity e) {
        return new KnownInvoicer(e.getId(), e.getUser().getId(), e.getEmailOrDomain(),
                e.getLabel(), e.getCreatedAt());
    }
}
