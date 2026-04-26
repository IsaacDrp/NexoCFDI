package mx.synectura.nexo_cfdi.modules.ingestion.api;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.MailAccountService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.LinkMailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.MailAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class MailAccountController {

    private final MailAccountService mailAccountService;

    @GetMapping("/mail-accounts")
    public List<MailAccountResponse> getMailAccounts(@AuthenticationPrincipal Jwt jwt) {
        return mailAccountService.getMailAccounts(jwt.getSubject());
    }

    @PostMapping("/mail-accounts/link")
    @ResponseStatus(HttpStatus.CREATED)
    public MailAccountResponse linkMailAccount(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody LinkMailRequest request) {
        return mailAccountService.linkMailAccount(jwt.getSubject(), request);
    }

    @DeleteMapping("/mail-accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkMailAccount(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID accountId) {
        mailAccountService.unlinkMailAccount(jwt.getSubject(), accountId);
    }

    @GetMapping("/mail-accounts/auth-url")
    public Map<String, String> getAuthorizationUrl(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam String provider) {
        String url = mailAccountService.getAuthorizationUrl(jwt.getSubject(), provider);
        return Map.of("authorizationUrl", url);
    }

    /** Callback permitido sin autenticación — Microsoft redirige aquí con el code */
    @GetMapping("/oauth2/callback/microsoft")
    public Map<String, String> microsoftCallback(@RequestParam String code,
                                                  @RequestParam String state) {
        // state = microsoftSub del usuario. El frontend debe completar el flujo
        // llamando a POST /mail-accounts/link con el code recibido.
        return Map.of("code", code, "state", state);
    }
}
