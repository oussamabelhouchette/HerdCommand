-- PA-006: searchable owner display name on membership. No Keycloak calls on the farm list.
-- H2 (MODE=PostgreSQL) and PostgreSQL: no gen_random_uuid(), no expression indexes.

ALTER TABLE farm_membership ADD COLUMN display_name VARCHAR(150);
