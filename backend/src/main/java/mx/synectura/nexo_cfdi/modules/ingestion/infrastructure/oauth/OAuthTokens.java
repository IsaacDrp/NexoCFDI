package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth;

public record OAuthTokens(
        String accessToken,
        String refreshToken,
        String emailAddress,
        long expiresIn
) {}
