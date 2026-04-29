package mx.synectura.nexo_cfdi.modules.ingestion.application;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.CreateKeywordRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UserKeywordResponse;

import java.util.List;
import java.util.UUID;

public interface UserKeywordService {
    List<UserKeywordResponse> findAll(String microsoftSub);
    UserKeywordResponse create(String microsoftSub, CreateKeywordRequest request);
    void delete(String microsoftSub, UUID keywordId);
}
