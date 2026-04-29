CREATE TYPE keyword_type AS ENUM ('INCLUDE', 'EXCLUDE');

CREATE TABLE user_keywords (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phrase     VARCHAR(255) NOT NULL,
    type       keyword_type NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_keywords_user_phrase_type UNIQUE (user_id, phrase, type)
);

CREATE INDEX idx_user_keywords_user_id ON user_keywords(user_id);
