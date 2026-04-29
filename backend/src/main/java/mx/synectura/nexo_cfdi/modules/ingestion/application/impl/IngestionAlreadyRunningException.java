package mx.synectura.nexo_cfdi.modules.ingestion.application.impl;

import mx.synectura.nexo_cfdi.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class IngestionAlreadyRunningException extends BaseException {
    public IngestionAlreadyRunningException(UUID userId, int year, int month) {
        super("Ya hay una ingestión en curso para user=%s %d/%d".formatted(userId, month, year),
                "INGESTION_RUNNING", HttpStatus.CONFLICT);
    }
}
