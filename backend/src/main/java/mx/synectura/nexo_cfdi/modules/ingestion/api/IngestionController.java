package mx.synectura.nexo_cfdi.modules.ingestion.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.IngestionService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestionRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    /** Dispara la ingestión asíncrona y devuelve el job_run RUNNING (HTTP 202). */
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobRunResponse triggerIngestion(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody IngestionRequest request) {
        return ingestionService.triggerIngestion(
                jwt.getSubject(), request.year(), request.month(), JobTrigger.REST);
    }

    /** Consulta el estado del job (RUNNING/SUCCESS/FAILED). */
    @GetMapping("/jobs/{jobId}")
    public JobRunResponse getJobStatus(@PathVariable UUID jobId) {
        return ingestionService.getJobStatus(jobId);
    }

    /** findAll de correos ingeridos del usuario para un mes/año. */
    @GetMapping("/emails")
    public List<IngestedEmailResponse> findEmails(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        return ingestionService.findIngestedEmails(jwt.getSubject(), year, month);
    }
}
