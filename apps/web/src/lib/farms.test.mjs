import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'farms.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/FarmSettings.tsx'), 'utf8');
const page = readFileSync(join(dir, '../app/[locale]/admin/farm-settings/page.tsx'), 'utf8');

test('farm list client calls list, summary, and details APIs', () => {
  assert.match(client, /\/api\/v1\/platform\/farms/);
  assert.match(client, /\/summary/);
  assert.match(client, /function listPlatformFarms/);
  assert.match(client, /function getPlatformFarmSummary/);
  assert.match(client, /function getPlatformFarm/);
  assert.match(client, /SEARCH_DEBOUNCE_MS = 400/);
  assert.match(client, /function farmListQueryFromSearchParams/);
  assert.match(client, /function farmListSearchParams/);
  assert.match(client, /FARM_STATUSES/);
  assert.match(client, /createdAt,desc/);
  assert.doesNotMatch(client, /HARRI/);
  assert.doesNotMatch(client, /@tanstack\/query/);
});

test('farm settings page loads list and summary from search params', () => {
  assert.match(page, /searchParams/);
  assert.match(page, /farmListQueryFromSearchParams/);
  assert.match(page, /listPlatformFarms/);
  assert.match(page, /getPlatformFarmSummary/);
  assert.match(page, /requirePlatformAdmin/);
  assert.match(page, /listFeatures\(token, locale, true\)/);
  assert.match(page, /features=\{features\.items\}/);
  assert.doesNotMatch(page, /ANIMAL_MANAGEMENT/);
});

test('farm table uses API rows, URL filters, and keeps write actions disabled', () => {
  assert.match(ui, /'use client'/);
  assert.match(ui, /SEARCH_DEBOUNCE_MS/);
  assert.match(ui, /router\.replace/);
  assert.match(ui, /farmListHref/);
  assert.match(ui, /getPlatformFarm\(/);
  assert.match(ui, /t\('openDetails'\)/);
  assert.match(ui, /wizardLater/);
  assert.match(ui, /editLater/);
  assert.match(ui, /suspendLater/);
  assert.match(ui, /skeleton/);
  assert.match(ui, /t\('empty'\)/);
  assert.match(ui, /t\('retry'\)/);
  assert.match(ui, /OwnerLookup/);
  assert.match(ui, /features\.map/);
  assert.match(ui, /data-farm-id=\{farm\.id\}/);
  assert.doesNotMatch(ui, /ANIMAL_MANAGEMENT/);
  assert.doesNotMatch(ui, /HARRI/);
  assert.doesNotMatch(ui, /@tanstack\/query/);
});
