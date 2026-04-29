package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.UserKeywordService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.CreateKeywordRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UserKeywordResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeywordRepository;
import mx.synectura.nexo_cfdi.shared.domain.user.User;
import mx.synectura.nexo_cfdi.shared.domain.user.UserRepository;
import mx.synectura.nexo_cfdi.shared.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserKeywordServiceImpl implements UserKeywordService {

    private final UserKeywordRepository keywordRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserKeywordResponse> findAll(String microsoftSub) {
        return keywordRepository.findAllByUserId(resolveUserId(microsoftSub)).stream()
                .map(UserKeywordResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public UserKeywordResponse create(String microsoftSub, CreateKeywordRequest request) {
        UUID userId = resolveUserId(microsoftSub);
        try {
            return UserKeywordResponse.from(
                    keywordRepository.save(userId, request.phrase().trim(), request.type()));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeywordException(request.phrase().trim());
        }
    }

    @Override
    public void delete(String microsoftSub, UUID keywordId) {
        UUID userId = resolveUserId(microsoftSub);
        if (!keywordRepository.delete(keywordId, userId)) {
            throw new NotFoundException("UserKeyword", keywordId);
        }
    }

    private UUID resolveUserId(String microsoftSub) {
        User user = userRepository.findByMicrosoftSub(microsoftSub)
                .orElseThrow(() -> new NotFoundException("User", microsoftSub));
        return user.id();
    }

    public static class DuplicateKeywordException extends mx.synectura.nexo_cfdi.shared.exception.BaseException {
        public DuplicateKeywordException(String phrase) {
            super("La palabra clave ya existe: " + phrase, "DUPLICATE_KEYWORD", HttpStatus.CONFLICT);
        }
    }
}
