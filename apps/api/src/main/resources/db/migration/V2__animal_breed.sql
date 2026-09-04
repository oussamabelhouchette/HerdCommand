CREATE TABLE animal_breed (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    species_code VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_animal_breed_display_order CHECK (display_order >= 0),
    CONSTRAINT ux_animal_breed_code UNIQUE (code)
);

CREATE INDEX ix_animal_breed_species_active_order ON animal_breed (species_code, active, display_order);
