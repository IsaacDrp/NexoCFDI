package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserKeywordJpaRepository extends JpaRepository<UserKeywordEntity, UUID> {

    List<UserKeywordEntity> findAllByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM UserKeywordEntity k WHERE k.id = :id AND k.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
