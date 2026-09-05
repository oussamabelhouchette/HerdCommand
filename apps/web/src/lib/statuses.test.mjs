import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const statuses = readFileSync(join(dir, 'statuses.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/StatusManagement.tsx'), 'utf8');
const settings = readFileSync(join(dir, '../components/admin/AnimalSettings.tsx'), 'utf8');

test('status client exposes the allowlisted color tokens only', () => {
  assert.match(statuses, /COLOR_TOKENS = \['success', 'purple', 'danger', 'warning', 'neutral'\]/);
  assert.match(statuses, /REQUIRED_STATUS_CODE = 'ACTIVE'/);
  assert.match(statuses, /function updateStatus/);
  assert.match(statuses, /function updateStatusActive/);
  assert.match(statuses, /function listStatuses/);
  assert.doesNotMatch(statuses, /function createStatus/);
  assert.doesNotMatch(statuses, /method: 'DELETE'/);
  assert.doesNotMatch(statuses, /method: 'POST'/);
});

test('status tab is wired on the animal-settings screen', () => {
  assert.match(settings, /StatusManagement/);
  assert.match(settings, /setTab\('statuses'\)/);
  assert.doesNotMatch(settings, /disabled title=\{t\('tabSoon'\)\}>\s*\{t\('tabStatuses'\)\}/);
});

test('edit modal keeps the technical code read-only and never submits it', () => {
  assert.match(ui, /id="status-code"/);
  assert.match(ui, /readOnly/);
  assert.match(ui, /disabled/);
  assert.match(ui, /updateStatus\(token, locale, editing\.code/);
  assert.doesNotMatch(ui, /code:\s*form/);
  assert.doesNotMatch(ui, /createStatus/);
  assert.doesNotMatch(ui, /method:\s*'DELETE'/);
});

test('live preview uses the current form label and color token without saving', () => {
  assert.match(ui, /data-testid="status-preview"/);
  assert.match(ui, /statusStyles\[form\.colorToken\]/);
  assert.match(ui, /statusLabel\(\{ labelAr: form\.labelAr, labelEn: form\.labelEn \}/);
  assert.match(ui, /COLOR_TOKENS\.map/);
  assert.match(ui, /visibleInFilter/);
});

test('ACTIVE cannot be deactivated in the UI', () => {
  assert.match(ui, /isRequiredStatus\(row\.code\)/);
  assert.match(ui, /isRequiredStatus\(editing\.code\)/);
  assert.match(ui, /cannotDeactivateActive/);
});
