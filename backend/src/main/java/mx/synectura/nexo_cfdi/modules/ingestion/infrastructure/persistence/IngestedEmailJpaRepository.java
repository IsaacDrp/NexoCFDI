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

    @Query("SELECT e FROM IngestedEmailEntity e WHERE e.user.id = :userId " +
            "AND e.cfdiFecha >= :from AND e.cfdiFecha < :to " +
            "AND e.processingStatus = 'STORED' ORDER BY e.cfdiFecha DESC")
    List<IngestedEmailEntity> findParsedCfdisByUserAndMonth(@Param("userId") UUID userId,
                                                            @Param("from") java.time.LocalDateTime from,
                                                            @Param("to") java.time.LocalDateTime to);

    @Query("SELECT a.storageKey FROM IngestedAttachmentEntity a " +
            "WHERE a.ingestedEmail.user.id = :userId " +
            "AND a.ingestedEmail.receivedAt >= :from AND a.ingestedEmail.receivedAt < :to " +
            "AND a.storageKey IS NOT NULL")
    List<String> findStorageKeysByUserAndPeriod(@Param("userId") UUID userId,
                                                @Param("from") OffsetDateTime from,
                                                @Param("to") OffsetDateTime to);

    boolean existsByMailAccountIdAndMessageId(UUID mailAccountId, String messageId);
}
