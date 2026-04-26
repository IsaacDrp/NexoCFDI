package mx.synectura.nexo_cfdi.shared.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(String resource, Object id) {
        super("%s con id '%s' no encontrado".formatted(resource, id), "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
