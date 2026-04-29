package mx.synectura.nexo_cfdi.modules.processor.domain;

public record CfdiData(
        String rfcEmisor,
        String rfcReceptor,
        String uuid
) {}
