package mx.synectura.nexo_cfdi.shared.domain.user;

import mx.synectura.nexo_cfdi.shared.domain.user.dto.RegisterUserRequest;
import mx.synectura.nexo_cfdi.shared.domain.user.dto.UserResponse;

public interface UserService {
    UserResponse registerOrUpdate(String microsoftSub, String email, RegisterUserRequest request);
    UserResponse getProfile(String microsoftSub);
}
