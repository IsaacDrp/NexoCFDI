package mx.synectura.nexo_cfdi.modules.processor.api;

import java.io.InputStream;
import java.util.Map;

public interface CfdiParserPort {
    Map<String, Object> parseXml(InputStream xmlContent);
    boolean validate(InputStream xmlContent);
}
