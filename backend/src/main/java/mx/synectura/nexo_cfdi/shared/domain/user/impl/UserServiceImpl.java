package mx.synectura.nexo_cfdi.shared.domain.user.impl;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import mx.synectura.nexo_cfdi.shared.domain.user.UserService;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.RegisterUserRequest;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.UserResponse;
import mx.synectura.nexo_cfdi.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse registerOrUpdate(String microsoftSub, String email, RegisterUserRequest req) {
        User existing = userRepository.findByMicrosoftSub(microsoftSub).orElse(null);

        User toSave = new User(
                existing != null ? existing.id() : null,
                microsoftSub,
                email,
                req.firstName(),
                req.paternalSurname(),
                req.maternalSurname(),
                req.rfc(),
                req.razonSocial(),
                req.postalCode(),
                req.personType(),
                req.regimenFiscal(),
                existing != null ? existing.createdAt() : null,
                null
        );

        return UserResponse.from(userRepository.save(toSave));
    }

    @Override
    public UserResponse getProfile(String microsoftSub) {
        return userRepository.findByMicrosoftSub(microsoftSub)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User", microsoftSub));
    }
}
