import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'features.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/FarmSettings.tsx'), 'utf8');
const page = readFileSync(join(dir, '../app/[locale]/admin/farm-settings/page.tsx'), 'utf8');

test('feature client loads the catalog API and stores selections by code', () => {
  assert.match(client, /\/api\/v1\/platform\/features/);
  assert.match(client, /includeComingSoon/);
  assert.match(client, /function listFeatures/);
  assert.match(client, /function selectedFeatureCodes/);
  assert.match(client, /feature\.enableable.*feature\.code/);
  assert.doesNotMatch(client, /animalManagementEnabled/);
  assert.doesNotMatch(client, /keycloak/i);
});

test('farm settings page loads the catalog instead of hardcoded feature cards', () => {
  assert.match(page, /listFeatures\(token, locale, true\)/);
  assert.match(page, /features=\{features\.items\}/);
  assert.doesNotMatch(page, /ANIMAL_MANAGEMENT/);
  assert.doesNotMatch(ui, /ANIMAL_MANAGEMENT/);
  assert.doesNotMatch(ui, /animalManagementEnabled/);
  assert.doesNotMatch(ui, /soonList/);
  assert.doesNotMatch(ui, /soonCreate/);
});

test('catalog items render from API props and coming-soon stays disabled', () => {
  assert.match(ui, /features\.map/);
  assert.match(ui, /data-feature-id=\{feature\.id\}/);
  assert.match(ui, /data-feature-code=\{feature\.code\}/);
  assert.match(ui, /value=\{feature\.code\}/);
  assert.match(ui, /disabled=\{disabled\}/);
  assert.match(ui, /!feature\.enableable/);
  assert.match(ui, /selectedFeatureCodes\(features\)/);
  assert.match(ui, /feature\.name/);
});
