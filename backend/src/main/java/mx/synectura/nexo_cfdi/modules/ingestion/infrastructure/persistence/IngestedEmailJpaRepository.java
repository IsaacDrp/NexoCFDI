package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IngestedEmailJpaRepository extends JpaRepository<IngestedEmailEntity, UUID> {

    @Query("SELECT e FROM IngestedEmailEntity e WHERE e.user.id = :userId " +
            "AND e.receivedAt >= :from AND e.receivedAt < :to ORDER BY e.receivedAt DESC")
    List<IngestedEmailEntity> findByUserAndReceivedBetween(@Param("userId") UUID userId,
                                                           @Param("from") OffsetDateTime from,
                                                           @Param("to") OffsetDateTime to);

    boolean existsByMailAccountIdAndMessageId(UUID mailAccountId, String messageId);
}
