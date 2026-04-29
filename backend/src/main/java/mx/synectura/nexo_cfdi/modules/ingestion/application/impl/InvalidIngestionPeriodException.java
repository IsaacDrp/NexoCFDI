package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import mx.synectura.nexo_cfdi.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidIngestionPeriodException extends BaseException {
    public InvalidIngestionPeriodException(String message) {
        super(message, "INVALID_PERIOD", HttpStatus.BAD_REQUEST);
    }
}
