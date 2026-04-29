package mx.synectura.nexo_cfdi.modules.processor.api;

import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiData;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiParseException;

public interface CfdiParserPort {
    /** Parsea el XML de un CFDI 3.3 o 4.0 y extrae los datos clave. */
    CfdiData parse(byte[] xmlContent) throws CfdiParseException;
}
