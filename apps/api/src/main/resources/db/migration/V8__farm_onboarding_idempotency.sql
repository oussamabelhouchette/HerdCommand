-- PA-005 atomic farm creation: idempotency and Keycloak compensation records.
-- H2 (MODE=PostgreSQL) and PostgreSQL: no gen_random_uuid(), no expression indexes.

CREATE TABLE farm_onboarding_request (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    farm_id UUID,
    created_identity_id VARCHAR(64),
    response_json VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ux_farm_onboarding_key UNIQUE (idempotency_key),
    CONSTRAINT ck_farm_onboarding_status CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED'))
);

CREATE INDEX ix_farm_onboarding_status ON farm_onboarding_request (status, created_at);

CREATE TABLE farm_onboarding_compensation (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    keycloak_user_id VARCHAR(64) NOT NULL,
    reason VARCHAR(200),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL
);

CREATE INDEX ix_farm_onboarding_compensation_open
    ON farm_onboarding_compensation (resolved, created_at);
