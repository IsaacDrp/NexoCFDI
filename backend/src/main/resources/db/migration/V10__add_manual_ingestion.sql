ALTER TABLE ingested_emails
    ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'EMAIL';

ALTER TABLE ingested_emails
    ALTER COLUMN mail_account_id DROP NOT NULL,
    ALTER COLUMN job_run_id      DROP NOT NULL,
    ALTER COLUMN message_id      DROP NOT NULL;
