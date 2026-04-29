package mx.synectura.nexo_cfdi.modules.processor.domain;

public class CfdiParseException extends RuntimeException {
    public CfdiParseException(String message) { super(message); }
    public CfdiParseException(String message, Throwable cause) { super(message, cause); }
}
