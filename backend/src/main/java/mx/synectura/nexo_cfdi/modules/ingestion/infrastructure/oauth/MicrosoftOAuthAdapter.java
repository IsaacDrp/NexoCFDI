package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailProvider;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.encryption.AesGcmTokenEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.Map;

@Component("MICROSOFT")
public class MicrosoftOAuthAdapter implements OAuthProviderPort {

    private static final String AUTHORITY      = "https://login.microsoftonline.com";
    private static final String TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private static final String AUTH_ENDPOINT  = "/oauth2/v2.0/authorize";

    private static final String SCOPE_STR = String.join(" ", new String[]{
            "openid", "profile", "email", "offline_access",
            "https://outlook.office.com/IMAP.AccessAsUser.All",
            "https://outlook.office.com/SMTP.Send"
    });

    @Value("${nexo.microsoft.client-id}")
    private String clientId;

    @Value("${nexo.microsoft.client-secret}")
    private String clientSecret;

    @Value("${nexo.microsoft.tenant-id}")
    private String tenantId;

    @Value("${nexo.microsoft.redirect-uri}")
    private String defaultRedirectUri;

    private final RestClient restClient;
    private final AesGcmTokenEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public MicrosoftOAuthAdapter(AesGcmTokenEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public MailProvider getProvider() {
        return MailProvider.MICROSOFT;
    }

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        return UriComponentsBuilder
                .fromUriString(AUTHORITY + "/" + tenantId + AUTH_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri != null ? redirectUri : defaultRedirectUri)
                .queryParam("response_mode", "query")
                .queryParam("scope", SCOPE_STR)
                .queryParam("state", state)
                .build().toUriString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthTokens exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri != null ? redirectUri : defaultRedirectUri);
        form.add("grant_type", "authorization_code");
        form.add("scope", SCOPE_STR);

        Map<String, Object> response = requestToken(form);
        String email = extractEmailFromIdToken((String) response.get("id_token"));
        return new OAuthTokens(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                email,
                ((Number) response.get("expires_in")).longValue()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthTokens refreshAccessToken(String encryptedRefreshToken) {
        String[] parts = encryptedRefreshToken.split(":", 2);
        String plainRefreshToken = encryptionService.decrypt(parts[0], parts[1]);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", plainRefreshToken);
        form.add("grant_type", "refresh_token");
        form.add("scope", SCOPE_STR);

        Map<String, Object> response = requestToken(form);
        String email = extractEmailFromIdToken((String) response.get("id_token"));
        return new OAuthTokens(
                (String) response.get("access_token"),
                (String) response.getOrDefault("refresh_token", plainRefreshToken),
                email,
                ((Number) response.get("expires_in")).longValue()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestToken(MultiValueMap<String, String> form) {
        return restClient.post()
                .uri(AUTHORITY + "/" + tenantId + TOKEN_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private String extractEmailFromIdToken(String idToken) {
        if (idToken == null) return "unknown";
        try {
            String[] parts = idToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            String email = (String) claims.get("email");
            if (email == null) email = (String) claims.get("preferred_username");
            return email != null ? email : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
