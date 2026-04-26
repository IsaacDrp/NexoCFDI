package mx.synectura.nexo_cfdi.modules.ingestion.api;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.LinkMailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.MailAccountResponse;

import java.util.List;
import java.util.UUID;

/** Port público del módulo ingestion — lo que otros módulos pueden invocar */
public interface MailAccountPort {
    List<MailAccountResponse> getMailAccounts(String microsoftSub);
    MailAccountResponse linkMailAccount(String microsoftSub, LinkMailRequest request);
    void unlinkMailAccount(String microsoftSub, UUID accountId);
}
