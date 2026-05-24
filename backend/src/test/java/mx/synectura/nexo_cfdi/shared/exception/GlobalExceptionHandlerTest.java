package mx.synectura.nexo_cfdi.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ------------------------------------------------------------------ BaseException (via NotFoundException)

    @Test
    void baseException_returns404WithNotFoundCode() {
        NotFoundException ex = new NotFoundException("Recurso", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        ResponseEntity<ApiError> response = handler.handleBase(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).contains("Recurso");
    }

    // ------------------------------------------------------------------ MethodArgumentNotValidException

    @Test
    void validationException_returns400WithValidationErrorAndFieldErrors() throws Exception {
        // Build binding result with one field error
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "nombre", "no debe estar vacío"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Validación fallida");
        assertThat(response.getBody().fieldErrors()).containsEntry("nombre", "no debe estar vacío");
    }

    @Test
    void validationException_twoErrorsSameField_keepsFirst() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "formato inválido"));
        bindingResult.addError(new FieldError("target", "email", "no debe estar vacío"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getBody().fieldErrors())
                .containsEntry("email", "formato inválido")
                .doesNotContainEntry("email", "no debe estar vacío");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
    }

    // ------------------------------------------------------------------ MethodArgumentTypeMismatchException

    @Test
    void typeMismatch_returns400WithInvalidParameterCode() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("year");

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_PARAMETER");
        assertThat(response.getBody().message()).contains("year");
    }

    // ------------------------------------------------------------------ MissingServletRequestParameterException

    @Test
    void missingParam_returns400WithMissingParameterCode() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("month", "int");

        ResponseEntity<ApiError> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().message()).contains("month");
    }

    // ------------------------------------------------------------------ HttpMessageNotReadableException

    @Test
    void notReadable_returns400WithMalformedRequestCode() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiError> response = handler.handleNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("Cuerpo de la petición ilegible o mal formado");
    }

    // ------------------------------------------------------------------ MaxUploadSizeExceededException

    @Test
    void maxUploadSize_returns413WithFileTooLargeCode() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024L);

        ResponseEntity<ApiError> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
        assertThat(response.getBody().message()).isEqualTo("El archivo excede el tamaño máximo permitido");
    }

    // ------------------------------------------------------------------ generic Exception (fallback)

    @Test
    void genericException_returns500WithInternalErrorCode() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<ApiError> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
    }

    // ------------------------------------------------------------------ fieldErrors null for non-validation errors

    @Test
    void nonValidationErrors_fieldErrorsIsNull() {
        Exception ex = new RuntimeException("boom");

        ResponseEntity<ApiError> response = handler.handleGeneric(ex);

        assertThat(response.getBody().fieldErrors()).isNull();
    }
}
