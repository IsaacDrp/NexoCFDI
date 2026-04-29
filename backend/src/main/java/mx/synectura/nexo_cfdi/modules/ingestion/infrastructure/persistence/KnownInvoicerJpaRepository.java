package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnownInvoicerJpaRepository extends JpaRepository<KnownInvoicerEntity, UUID> {
    List<KnownInvoicerEntity> findAllByUserId(UUID userId);
}
