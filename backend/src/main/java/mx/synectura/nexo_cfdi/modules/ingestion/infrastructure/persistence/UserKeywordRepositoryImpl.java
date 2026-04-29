package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KeywordType;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeyword;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeywordRepository;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserKeywordRepositoryImpl implements UserKeywordRepository {

    private final UserKeywordJpaRepository jpa;
    private final UserJpaRepository userJpa;

    @Override
    public List<UserKeyword> findAllByUserId(UUID userId) {
        return jpa.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserKeyword save(UUID userId, String phrase, KeywordType type) {
        UserKeywordEntity entity = new UserKeywordEntity();
        entity.setUser(userJpa.getReferenceById(userId));
        entity.setPhrase(phrase);
        entity.setType(type);
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID userId) {
        return jpa.deleteByIdAndUserId(id, userId) > 0;
    }

    private UserKeyword toDomain(UserKeywordEntity e) {
        return new UserKeyword(e.getId(), e.getUser().getId(), e.getPhrase(), e.getType(), e.getCreatedAt());
    }
}
