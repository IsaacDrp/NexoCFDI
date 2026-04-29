package mx.synectura.nexo_cfdi.shared.api;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.shared.domain.user.UserService;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.RegisterUserRequest;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<UserResponse> upsertProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegisterUserRequest req) {
        
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("preferred_username"); // Microsoft email claim
        if (email == null) email = jwt.getClaimAsString("email");

        return ResponseEntity.ok(userService.registerOrUpdate(sub, email, req));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfile(jwt.getSubject()));
    }
}
