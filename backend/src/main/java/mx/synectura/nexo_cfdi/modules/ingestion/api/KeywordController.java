package mx.synectura.nexo_cfdi.modules.ingestion.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.UserKeywordService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.CreateKeywordRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UserKeywordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final UserKeywordService keywordService;

    @GetMapping
    public List<UserKeywordResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return keywordService.findAll(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserKeywordResponse create(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CreateKeywordRequest request) {
        return keywordService.create(jwt.getSubject(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        keywordService.delete(jwt.getSubject(), id);
    }
}
