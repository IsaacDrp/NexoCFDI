package mx.synectura.nexo_cfdi.modules.ingestion.application;

import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.IngestedEmailResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.application.dto.JobRunResponse;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;

import java.util.List;
import java.util.UUID;

public interface IngestionService {

    /** Encola la ingestión asíncrona. Devuelve el job_run creado en estado RUNNING. */
    JobRunResponse triggerIngestion(String microsoftSub, int year, int month, JobTrigger trigger);

    /** Consulta el estado de un job_run. */
    JobRunResponse getJobStatus(UUID jobRunId);

    /** findAll de correos ingeridos por usuario y mes/año. */
    List<IngestedEmailResponse> findIngestedEmails(String microsoftSub, int year, int month);
}
