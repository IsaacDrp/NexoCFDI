package mx.synectura.nexo_cfdi.modules.ingestion.application;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.ManualCfdiRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.UpdateIngestedEmailRequest;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IngestionService {

    /** Encola la ingestión asíncrona. Devuelve el job_run creado en estado RUNNING. */
    JobRunResponse triggerIngestion(String microsoftSub, int year, int month, JobTrigger trigger);

    /** Consulta el estado de un job_run. */
    JobRunResponse getJobStatus(UUID jobRunId);

    /** findAll de correos ingeridos por usuario y mes/año. */
    List<IngestedEmailResponse> findIngestedEmails(String microsoftSub, int year, int month);

    /** Registra un CFDI de forma manual, con archivo PDF/ZIP opcional. */
    IngestedEmailResponse addManualEntry(String microsoftSub, ManualCfdiRequest request, MultipartFile file);

    /** Edita los campos CFDI de un correo ingerido y opcionalmente sube un PDF o XML. */
    IngestedEmailResponse updateEmail(String microsoftSub, UUID emailId,
                                      UpdateIngestedEmailRequest req, MultipartFile file);

    /** Genera una URL temporal para descargar/ver el archivo en MinIO. */
    String getAttachmentPreviewUrl(String microsoftSub, UUID emailId, UUID attachmentId);
}
