import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'farms.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/FarmSettings.tsx'), 'utf8');
const page = readFileSync(join(dir, '../app/[locale]/admin/farm-settings/page.tsx'), 'utf8');
const ar = readFileSync(join(dir, '../messages/ar.json'), 'utf8');
const en = readFileSync(join(dir, '../messages/en.json'), 'utf8');

test('farm client calls list, summary and details APIs and never Keycloak', () => {
  assert.match(client, /\/api\/v1\/platform\/farms/);
  assert.match(client, /\$\{BASE\}\/summary/);
  assert.match(client, /\$\{BASE\}\/\$\{farmId\}/);
  assert.match(client, /function listPlatformFarms/);
  assert.match(client, /function getPlatformFarmSummary/);
  assert.match(client, /function getPlatformFarm/);
  assert.match(client, /function farmListQueryFromSearchParams/);
  assert.match(client, /function farmListSearchParams/);
  assert.match(client, /function farmListHref/);
  assert.match(client, /SEARCH_DEBOUNCE_MS = 400/);
  assert.match(client, /FARM_STATUSES/);
  assert.match(client, /function createPlatformFarm/);
  assert.doesNotMatch(client, /keycloak/i);
  assert.doesNotMatch(client, /admin-cli/);
});

test('farm-settings page is platform-admin only and hydrates from URL plus APIs', () => {
  assert.match(page, /requirePlatformAdmin/);
  assert.match(page, /farmListQueryFromSearchParams/);
  assert.match(page, /listPlatformFarms/);
  assert.match(page, /getPlatformFarmSummary/);
  assert.match(page, /listFeatures\(token, locale, true\)/);
  assert.doesNotMatch(page, /probePlatformFarms/);
});

test('farm table loads API rows, filters by codes, and keeps query params', () => {
  assert.match(ui, /SEARCH_DEBOUNCE_MS/);
  assert.match(ui, /listPlatformFarms/);
  assert.match(ui, /getPlatformFarmSummary/);
  assert.match(ui, /getPlatformFarm\(/);
  assert.match(ui, /farmListHref/);
  assert.match(ui, /router\.replace/);
  assert.match(ui, /value=\{code\}/);
  assert.match(ui, /value=\{feature\.code\}/);
  assert.match(ui, /data-farm-id=\{farm\.id\}/);
  assert.match(ui, /data-farm-code=\{farm\.code\}/);
  assert.match(ui, /data-feature-code=\{code\}/);
  assert.match(ui, /data-loading="true"/);
  assert.match(ui, /t\('empty'\)/);
  assert.match(ui, /t\('retry'\)/);
  assert.match(ui, /setPage\(\(value\) => value \+ 1\)/);
  assert.match(ui, /FarmCreateWizard/);
  assert.match(ui, /setWizardOpen\(true\)/);
  assert.doesNotMatch(ui, /الوادي الأخضر|Green Valley|الزيتونة/);
});

test('row actions expose details now and leave edit or suspend for later stories', () => {
  assert.match(ui, /openDetails/);
  assert.match(ui, /disabled title=\{t\('editLater'\)\}/);
  assert.match(ui, /disabled\s+title=\{t\('suspendLater'\)\}/);
  assert.match(ui, /onClick=\{\(\) => setWizardOpen\(true\)\}/);
  assert.match(ui, /OwnerLookup/);
});

test('Arabic and English localize status and plan codes without hardcoding farms', () => {
  assert.match(ar, /"SETUP": "قيد الإعداد"/);
  assert.match(ar, /"ACTIVE": "نشطة"/);
  assert.match(en, /"SETUP": "Setup"/);
  assert.match(en, /"ESSENTIAL": "Essential"/);
  assert.doesNotMatch(ar, /الوادي الأخضر/);
  assert.doesNotMatch(en, /Green Valley Farm/);
});
