package mx.synectura.nexo_cfdi.shared.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByMicrosoftSub(String microsoftSub);
    User save(User user);
}
