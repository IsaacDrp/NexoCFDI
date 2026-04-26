package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailProvider;

public interface OAuthProviderPort {
    MailProvider getProvider();
    String buildAuthorizationUrl(String state, String redirectUri);
    OAuthTokens exchangeCode(String code, String redirectUri);
    OAuthTokens refreshAccessToken(String encryptedRefreshToken);
}
