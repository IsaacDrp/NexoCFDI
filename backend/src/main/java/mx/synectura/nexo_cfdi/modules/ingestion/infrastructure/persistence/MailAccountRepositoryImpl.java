package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailAccount;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailAccountRepository;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.SyncStatus;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MailAccountRepositoryImpl implements MailAccountRepository {

    private final MailAccountJpaRepository jpa;
    private final UserJpaRepository userJpa;

    @Override
    public List<MailAccount> findAllByUserId(UUID userId) {
        return jpa.findAllByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<MailAccount> findByIdAndUserId(UUID id, UUID userId) {
        return jpa.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    @Transactional
    public MailAccount save(MailAccount account, String encryptedRefreshToken, String tokenIv) {
        MailAccountEntity entity = new MailAccountEntity();
        entity.setUser(userJpa.getReferenceById(account.userId()));
        entity.setDisplayName(account.displayName());
        entity.setEmailAddress(account.emailAddress());
        entity.setProvider(account.provider());
        entity.setStatus(account.status());
        entity.setEncryptedRefreshToken(encryptedRefreshToken);
        entity.setTokenIv(tokenIv);
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, SyncStatus status, String errorMessage) {
        jpa.updateStatus(id, status, errorMessage);
    }

    @Override
    @Transactional
    public void updateLastSyncAt(UUID id) {
        jpa.updateLastSyncAt(id, OffsetDateTime.now());
    }

    @Override
    public List<UUID> findDistinctUserIdsByStatus(SyncStatus status) {
        return jpa.findDistinctUserIdsByStatus(status);
    }

    private MailAccount toDomain(MailAccountEntity e) {
        return new MailAccount(e.getId(), e.getUser().getId(), e.getDisplayName(),
                e.getEmailAddress(), e.getProvider(), e.getStatus(),
                e.getLastSyncAt(), e.getCreatedAt());
    }
}
