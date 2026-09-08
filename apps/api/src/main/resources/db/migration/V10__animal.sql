-- Farm-scoped animals. Breeds and statuses stay shared reference data.

CREATE TABLE animal (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farm (id),
    identification_number VARCHAR(40) NOT NULL,
    name VARCHAR(100),
    breed_id UUID NOT NULL REFERENCES animal_breed (id),
    status_code VARCHAR(40) NOT NULL REFERENCES animal_status_definition (code),
    gender_code VARCHAR(20) NOT NULL,
    date_of_birth DATE,
    group_id UUID REFERENCES animal_group (id),
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_animal_gender CHECK (gender_code IN ('FEMALE', 'MALE')),
    CONSTRAINT ck_animal_identification CHECK (identification_number <> ''),
    CONSTRAINT ux_animal_farm_identification UNIQUE (farm_id, identification_number)
);

CREATE INDEX ix_animal_farm_created ON animal (farm_id, created_at);
CREATE INDEX ix_animal_farm_group ON animal (farm_id, group_id);
CREATE INDEX ix_animal_farm_breed ON animal (farm_id, breed_id);
CREATE INDEX ix_animal_farm_status ON animal (farm_id, status_code);
CREATE INDEX ix_animal_farm_gender ON animal (farm_id, gender_code);
CREATE INDEX ix_animal_farm_archived ON animal (farm_id, archived_at);
