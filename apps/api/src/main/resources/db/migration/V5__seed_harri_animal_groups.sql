-- Demo groups from the animal-list mock. All active. Type stays OTHER until animal stories use it.

INSERT INTO animal_group (
    id, farm_id, code, name_ar, name_en, group_type_code, description, capacity, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    f.id,
    'HARRI-DAMS',
    'أمهات حري',
    'Harri mothers',
    'OTHER',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm f
WHERE f.code = 'HARRI'
  AND NOT EXISTS (
      SELECT 1 FROM animal_group g WHERE g.farm_id = f.id AND g.code = 'HARRI-DAMS'
  );

INSERT INTO animal_group (
    id, farm_id, code, name_ar, name_en, group_type_code, description, capacity, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'b2eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    f.id,
    'FARM-SIRES',
    'فحول المزرعة',
    'Farm sires',
    'OTHER',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm f
WHERE f.code = 'HARRI'
  AND NOT EXISTS (
      SELECT 1 FROM animal_group g WHERE g.farm_id = f.id AND g.code = 'FARM-SIRES'
  );

INSERT INTO animal_group (
    id, farm_id, code, name_ar, name_en, group_type_code, description, capacity, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'b3eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    f.id,
    'PRODUCTION',
    'مجموعة الإنتاج',
    'Production group',
    'OTHER',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm f
WHERE f.code = 'HARRI'
  AND NOT EXISTS (
      SELECT 1 FROM animal_group g WHERE g.farm_id = f.id AND g.code = 'PRODUCTION'
  );

INSERT INTO animal_group (
    id, farm_id, code, name_ar, name_en, group_type_code, description, capacity, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'b4eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    f.id,
    'GROUP-B',
    'مجموعة ب',
    'Group B',
    'OTHER',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm f
WHERE f.code = 'HARRI'
  AND NOT EXISTS (
      SELECT 1 FROM animal_group g WHERE g.farm_id = f.id AND g.code = 'GROUP-B'
  );

INSERT INTO animal_group (
    id, farm_id, code, name_ar, name_en, group_type_code, description, capacity, active,
    created_at, created_by, updated_at, updated_by
)
SELECT
    'b5eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    f.id,
    'GROUP-C',
    'مجموعة ج',
    'Group C',
    'OTHER',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
FROM farm f
WHERE f.code = 'HARRI'
  AND NOT EXISTS (
      SELECT 1 FROM animal_group g WHERE g.farm_id = f.id AND g.code = 'GROUP-C'
  );
