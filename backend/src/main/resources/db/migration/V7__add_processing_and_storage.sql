-- Estado de procesamiento (parseo + validación + almacenamiento) por correo
ALTER TABLE ingested_emails
    ADD COLUMN processing_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN error_cause        TEXT,
    ADD COLUMN cfdi_uuid          VARCHAR(36);

-- Clave del objeto en el blob store (nula si no fue almacenado)
ALTER TABLE ingested_attachments
    ADD COLUMN storage_key VARCHAR(1024);
