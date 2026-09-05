-- US-AC-006 farm-owned groups. One seeded farm for local/admin UI.
-- Tests create extra farms; do not seed a second farm here.

CREATE TABLE farm (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ux_farm_code UNIQUE (code)
);

INSERT INTO farm (id, code, name_ar, name_en, active, created_at, created_by, updated_at, updated_by)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'HARRI',
    'مزرعة حري',
    'Harri Farm',
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
);

CREATE TABLE animal_group (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farm (id),
    code VARCHAR(40) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    group_type_code VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    capacity INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_animal_group_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ux_animal_group_farm_code UNIQUE (farm_id, code)
);

CREATE INDEX ix_animal_group_farm_active_type ON animal_group (farm_id, active, group_type_code);
