-- Tipos enumerados
CREATE TYPE ingestion_job_trigger AS ENUM ('REST', 'SCHEDULED');
CREATE TYPE ingestion_job_status  AS ENUM ('RUNNING', 'SUCCESS', 'FAILED');

-- Catálogo de remitentes reconocidos como facturadores (por usuario)
CREATE TABLE known_invoicers (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_or_domain  VARCHAR(255) NOT NULL,
    label            VARCHAR(150),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_known_invoicers_user_value UNIQUE (user_id, email_or_domain)
);

CREATE INDEX idx_known_invoicers_user_id ON known_invoicers(user_id);

-- Bitácora de ejecuciones del proceso de ingestión
CREATE TABLE ingestion_job_runs (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    triggered_by           ingestion_job_trigger NOT NULL,
    triggered_by_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    target_year            INTEGER NOT NULL,
    target_month           INTEGER NOT NULL CHECK (target_month BETWEEN 1 AND 12),
    started_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at            TIMESTAMPTZ,
    status                 ingestion_job_status NOT NULL DEFAULT 'RUNNING',
    accounts_total         INTEGER NOT NULL DEFAULT 0,
    accounts_ok            INTEGER NOT NULL DEFAULT 0,
    accounts_failed        INTEGER NOT NULL DEFAULT 0,
    emails_ingested        INTEGER NOT NULL DEFAULT 0,
    error_message          TEXT
);

CREATE INDEX idx_job_runs_user_period
    ON ingestion_job_runs(triggered_by_user_id, target_year, target_month, status);

-- Bitácora de correos identificados como facturas
CREATE TABLE ingested_emails (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mail_account_id   UUID NOT NULL REFERENCES mail_accounts(id) ON DELETE CASCADE,
    job_run_id        UUID NOT NULL REFERENCES ingestion_job_runs(id) ON DELETE CASCADE,
    message_id        VARCHAR(512) NOT NULL,
    subject           TEXT,
    from_address      VARCHAR(320),
    received_at       TIMESTAMPTZ NOT NULL,
    has_zip           BOOLEAN NOT NULL DEFAULT FALSE,
    has_xml           BOOLEAN NOT NULL DEFAULT FALSE,
    has_pdf           BOOLEAN NOT NULL DEFAULT FALSE,
    match_reasons     JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ingested_emails_account_message UNIQUE (mail_account_id, message_id)
);

CREATE INDEX idx_ingested_emails_user_received ON ingested_emails(user_id, received_at);
CREATE INDEX idx_ingested_emails_job_run       ON ingested_emails(job_run_id);

-- Metadata de archivos ingeridos (sin contenido binario)
CREATE TABLE ingested_attachments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingested_email_id   UUID NOT NULL REFERENCES ingested_emails(id) ON DELETE CASCADE,
    filename            VARCHAR(512) NOT NULL,
    extension           VARCHAR(20) NOT NULL,
    size_bytes          BIGINT NOT NULL DEFAULT 0,
    inside_zip          BOOLEAN NOT NULL DEFAULT FALSE,
    parent_zip_name     VARCHAR(512),
    depth               SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingested_attachments_email ON ingested_attachments(ingested_email_id);
