package mx.synectura.nexo_cfdi.shared.domain.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByMicrosoftSub(String microsoftSub);
}
