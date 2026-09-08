-- PA-002 farm tenant model. Evolves the US-AC-006 farm table; does not replace it.
-- H2 (MODE=PostgreSQL) and PostgreSQL: no gen_random_uuid(), no expression indexes.

ALTER TABLE farm ALTER COLUMN name_ar TYPE VARCHAR(150);
ALTER TABLE farm ALTER COLUMN name_en TYPE VARCHAR(150);

ALTER TABLE farm ADD COLUMN name_fr VARCHAR(150);
UPDATE farm SET name_fr = name_en WHERE name_fr IS NULL;
ALTER TABLE farm ALTER COLUMN name_fr SET NOT NULL;

ALTER TABLE farm ADD COLUMN governorate_code VARCHAR(10) NOT NULL DEFAULT 'TN-11';
ALTER TABLE farm ADD COLUMN address VARCHAR(500);
ALTER TABLE farm ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Africa/Tunis';
ALTER TABLE farm ADD COLUMN default_language VARCHAR(8) NOT NULL DEFAULT 'ar';
ALTER TABLE farm ADD COLUMN currency_code VARCHAR(8) NOT NULL DEFAULT 'TND';
ALTER TABLE farm ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE farm ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE farm ADD CONSTRAINT ck_farm_default_language CHECK (default_language IN ('ar', 'fr'));
ALTER TABLE farm ADD CONSTRAINT ck_farm_currency CHECK (currency_code = 'TND');
ALTER TABLE farm ADD CONSTRAINT ck_farm_status CHECK (status IN ('SETUP', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'));
ALTER TABLE farm ADD CONSTRAINT ck_farm_timezone CHECK (timezone <> '');

CREATE SEQUENCE farm_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE farm_membership (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farm (id),
    keycloak_user_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_email VARCHAR(320) NOT NULL,
    invited_at TIMESTAMP WITH TIME ZONE,
    invited_by VARCHAR(64),
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_farm_membership_role CHECK (role_code IN ('FARM_OWNER')),
    CONSTRAINT ck_farm_membership_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED')),
    CONSTRAINT ux_farm_membership_user UNIQUE (farm_id, keycloak_user_id)
);

CREATE INDEX ix_farm_membership_farm_status ON farm_membership (farm_id, status);

CREATE TABLE farm_subscription (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farm (id),
    plan_code VARCHAR(30) NOT NULL,
    max_active_animals INTEGER NOT NULL,
    max_team_members INTEGER NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_farm_subscription_plan CHECK (plan_code IN ('TRIAL', 'ESSENTIAL', 'PROFESSIONAL')),
    CONSTRAINT ck_farm_subscription_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_farm_subscription_animals CHECK (max_active_animals > 0),
    CONSTRAINT ck_farm_subscription_team CHECK (max_team_members > 0),
    CONSTRAINT ux_farm_subscription_farm UNIQUE (farm_id)
);

INSERT INTO farm_subscription (
    id,
    farm_id,
    plan_code,
    max_active_animals,
    max_team_members,
    trial_ends_at,
    status,
    version,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    id,
    'ESSENTIAL',
    500,
    10,
    NULL,
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm
WHERE code = 'HARRI';
