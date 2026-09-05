CREATE TABLE animal_status_definition (
    code VARCHAR(40) PRIMARY KEY,
    label_ar VARCHAR(100) NOT NULL,
    label_en VARCHAR(100) NOT NULL,
    color_token VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    visible_in_filter BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_protected BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    CONSTRAINT ck_animal_status_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_animal_status_color_token CHECK (
        color_token IN ('success', 'purple', 'danger', 'warning', 'neutral')
    )
);

CREATE INDEX ix_animal_status_active_order ON animal_status_definition (active, display_order);

INSERT INTO animal_status_definition (
    code, label_ar, label_en, color_token, display_order, visible_in_filter, active, system_protected,
    created_at, created_by, updated_at, updated_by
) VALUES
    ('ACTIVE', 'نشط', 'Active', 'success', 10, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system'),
    ('PREGNANT', 'حامل', 'Pregnant', 'purple', 20, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system'),
    ('SICK', 'مريض', 'Sick', 'danger', 30, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system'),
    ('ISOLATED', 'معزول', 'Isolated', 'warning', 40, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system');
