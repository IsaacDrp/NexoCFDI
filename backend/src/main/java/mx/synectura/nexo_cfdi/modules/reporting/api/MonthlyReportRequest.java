package mx.synectura.nexo_cfdi.modules.reporting.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MonthlyReportRequest(
        @NotBlank @Email String emailDestino,
        @NotBlank String asunto,
        String mensajePersonalizado
) {}
