package mx.synectura.nexo_cfdi.shared.domain.user.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByMicrosoftSub(String microsoftSub) {
        return jpa.findByMicrosoftSub(microsoftSub).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        return toDomain(jpa.save(entity));
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getMicrosoftSub(), e.getEmail(), e.getFirstName(),
                e.getPaternalSurname(), e.getMaternalSurname(), e.getRfc(),
                e.getRazonSocial(), e.getPostalCode(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private UserEntity toEntity(User u) {
        UserEntity e = new UserEntity();
        e.setMicrosoftSub(u.microsoftSub());
        e.setEmail(u.email());
        e.setFirstName(u.firstName());
        e.setPaternalSurname(u.paternalSurname());
        e.setMaternalSurname(u.maternalSurname());
        e.setRfc(u.rfc());
        e.setRazonSocial(u.razonSocial());
        e.setPostalCode(u.postalCode());
        return e;
    }
}
