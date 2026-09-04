-- US-AC-001 configuration foundation.
-- Later stories add breed, status, and group tables using these audit columns:
-- created_at, created_by, updated_at, updated_by.

CREATE TABLE herdcommand_schema_info (
    id INTEGER PRIMARY KEY,
    module VARCHAR(64) NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO herdcommand_schema_info (id, module, applied_at)
VALUES (1, 'configuration-foundation', CURRENT_TIMESTAMP);
