package mx.synectura.nexo_cfdi.modules.ingestion.application;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.LinkMailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.MailAccountResponse;

import java.util.List;
import java.util.UUID;

public interface MailAccountService {
    List<MailAccountResponse> getMailAccounts(String microsoftSub);
    MailAccountResponse linkMailAccount(String microsoftSub, LinkMailRequest request);
    void unlinkMailAccount(String microsoftSub, UUID accountId);
    String getAuthorizationUrl(String microsoftSub, String provider);
}
