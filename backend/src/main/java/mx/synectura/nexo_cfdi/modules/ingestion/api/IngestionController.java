package mx.synectura.nexo_cfdi.modules.ingestion.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.synectura.nexo_cfdi.modules.ingestion.application.IngestionService;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestionRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.ManualCfdiRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.PresignedUrlResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UpdateIngestedEmailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /** Registra un CFDI manualmente, con archivo PDF/ZIP opcional. */
    @PostMapping(value = "/emails/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestedEmailResponse addManualEntry(
            @AuthenticationPrincipal Jwt jwt,
            @ModelAttribute ManualCfdiRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ingestionService.addManualEntry(jwt.getSubject(), request, file);
    }

    /** Edita los campos CFDI de un correo ingerido y opcionalmente sube un PDF o XML. */
    @PutMapping(value = "/emails/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestedEmailResponse updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @ModelAttribute UpdateIngestedEmailRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ingestionService.updateEmail(jwt.getSubject(), id, request, file);
    }

    /** Obtiene una URL pre-firmada para previsualizar/descargar un adjunto almacenado en MinIO. */
    @GetMapping("/emails/{emailId}/attachments/{attachmentId}/preview")
    public PresignedUrlResponse getAttachmentPreviewUrl(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID emailId,
            @PathVariable UUID attachmentId) {
        return new PresignedUrlResponse(ingestionService.getAttachmentPreviewUrl(jwt.getSubject(), emailId, attachmentId));
    }
}
