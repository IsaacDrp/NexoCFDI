CREATE TYPE mail_provider AS ENUM ('MICROSOFT', 'GOOGLE');
CREATE TYPE sync_status   AS ENUM ('ACTIVE', 'PAUSED', 'REVOKED', 'ERROR');

CREATE TABLE mail_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name            VARCHAR(100),
    email_address           VARCHAR(255) NOT NULL,
    provider                mail_provider NOT NULL,
    status                  sync_status   NOT NULL DEFAULT 'ACTIVE',
    encrypted_refresh_token TEXT NOT NULL,
    token_iv                VARCHAR(24) NOT NULL,
    last_sync_at            TIMESTAMPTZ,
    sync_error_message      TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mail_accounts_user_id ON mail_accounts(user_id);
