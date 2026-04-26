package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailProvider;

public record LinkMailRequest(
        String authorizationCode,
        String redirectUri,
        String displayName,
        MailProvider provider
) {}
