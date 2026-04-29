package mx.synectura.nexo_cfdi.modules.ingestion.domain;

import java.util.List;
import java.util.UUID;

public interface UserKeywordRepository {
    List<UserKeyword> findAllByUserId(UUID userId);
    UserKeyword save(UUID userId, String phrase, KeywordType type);
    /** @return true si existía y fue eliminada, false si no pertenecía al usuario. */
    boolean delete(UUID id, UUID userId);
}
