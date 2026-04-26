package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.MailAccountService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.LinkMailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.MailAccountResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.*;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.encryption.AesGcmTokenEncryptionService;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthProviderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthTokens;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import mx.synectura.nexo_cfdi.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailAccountServiceImpl implements MailAccountService {

    private final UserRepository userRepository;
    private final MailAccountRepository mailAccountRepository;
    private final AesGcmTokenEncryptionService encryptionService;
    /** All OAuth2 providers, keyed by MailProvider name */
    private final Map<String, OAuthProviderPort> oauthProviders;

    @Override
    public List<MailAccountResponse> getMailAccounts(String microsoftSub) {
        User user = resolveUser(microsoftSub);
        return mailAccountRepository.findAllByUserId(user.id())
                .stream()
                .map(MailAccountResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MailAccountResponse linkMailAccount(String microsoftSub, LinkMailRequest request) {
        User user = resolveUser(microsoftSub);
        OAuthProviderPort provider = resolveProvider(request.provider());

        OAuthTokens tokens = provider.exchangeCode(request.authorizationCode(), request.redirectUri());
        AesGcmTokenEncryptionService.EncryptedToken encrypted = encryptionService.encrypt(tokens.refreshToken());

        MailAccount account = new MailAccount(
                null,
                user.id(),
                request.displayName(),
                tokens.emailAddress(),
                request.provider(),
                SyncStatus.ACTIVE,
                null,
                null
        );

        MailAccount saved = mailAccountRepository.save(account, encrypted.ciphertext(), encrypted.iv());
        return MailAccountResponse.from(saved);
    }

    @Override
    @Transactional
    public void unlinkMailAccount(String microsoftSub, UUID accountId) {
        User user = resolveUser(microsoftSub);
        mailAccountRepository.findByIdAndUserId(accountId, user.id())
                .orElseThrow(() -> new NotFoundException("MailAccount", accountId));
        mailAccountRepository.updateStatus(accountId, SyncStatus.REVOKED, null);
    }

    @Override
    public String getAuthorizationUrl(String microsoftSub, String providerName) {
        MailProvider provider = MailProvider.valueOf(providerName.toUpperCase());
        String state = microsoftSub;
        return resolveProvider(provider).buildAuthorizationUrl(state, null);
    }

    private User resolveUser(String microsoftSub) {
        return userRepository.findByMicrosoftSub(microsoftSub)
                .orElseThrow(() -> new NotFoundException("User", microsoftSub));
    }

    private OAuthProviderPort resolveProvider(MailProvider provider) {
        OAuthProviderPort port = oauthProviders.get(provider.name());
        if (port == null) {
            throw new IllegalArgumentException("Proveedor OAuth2 no soportado: " + provider);
        }
        return port;
    }
}
