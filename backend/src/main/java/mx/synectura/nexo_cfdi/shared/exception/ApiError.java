package mx.synectura.nexo_cfdi.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        String timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, OffsetDateTime.now().toString(), null);
    }

    public static ApiError of(String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(code, message, OffsetDateTime.now().toString(), fieldErrors);
    }
}
