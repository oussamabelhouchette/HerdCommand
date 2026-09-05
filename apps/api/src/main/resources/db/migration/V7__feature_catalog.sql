-- PA-003 dynamic feature catalog. No feature flags on farm.
-- H2 (MODE=PostgreSQL) and PostgreSQL: no gen_random_uuid(), no expression indexes.
-- Seed is idempotent so the same insert can be re-run safely.

CREATE TABLE feature_catalog (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    name_ar VARCHAR(150) NOT NULL,
    name_en VARCHAR(150) NOT NULL,
    name_fr VARCHAR(150) NOT NULL,
    description_ar VARCHAR(500),
    description_en VARCHAR(500),
    description_fr VARCHAR(500),
    icon_code VARCHAR(40),
    release_status VARCHAR(20) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ux_feature_catalog_code UNIQUE (code),
    CONSTRAINT ck_feature_catalog_status CHECK (release_status IN ('AVAILABLE', 'COMING_SOON', 'RETIRED')),
    CONSTRAINT ck_feature_catalog_display_order CHECK (display_order >= 0)
);

CREATE INDEX ix_feature_catalog_active_order ON feature_catalog (active, display_order, code);

CREATE TABLE farm_feature (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farm (id),
    feature_id UUID NOT NULL REFERENCES feature_catalog (id),
    enabled BOOLEAN NOT NULL,
    enabled_at TIMESTAMP WITH TIME ZONE,
    enabled_by VARCHAR(64),
    configuration_json VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ux_farm_feature UNIQUE (farm_id, feature_id)
);

CREATE INDEX ix_farm_feature_farm ON farm_feature (farm_id, enabled);

INSERT INTO feature_catalog (
    id, code, name_ar, name_en, name_fr,
    description_ar, description_en, description_fr,
    icon_code, release_status, display_order, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'ANIMAL_MANAGEMENT',
    'إدارة الحيوانات',
    'Animal management',
    'Gestion des animaux',
    'تسجيل الحيوانات والمجموعات والحالات.',
    'Register animals, groups, and statuses.',
    'Enregistrer les animaux, les groupes et les statuts.',
    'paw',
    'AVAILABLE',
    10,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM feature_catalog WHERE code = 'ANIMAL_MANAGEMENT'
);

INSERT INTO feature_catalog (
    id, code, name_ar, name_en, name_fr,
    description_ar, description_en, description_fr,
    icon_code, release_status, display_order, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'c3eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'HEALTH',
    'السجلات الصحية',
    'Health records',
    'Dossiers de santé',
    'لقاحات وعلاجات — غير متاح بعد.',
    'Vaccines and treatments — not released yet.',
    'Vaccins et traitements — pas encore disponible.',
    'heart',
    'COMING_SOON',
    20,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM feature_catalog WHERE code = 'HEALTH'
);
