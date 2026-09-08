import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const source = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'portals.ts'), 'utf8');

test('administrator group maps to /admin and the default portal is /portal', () => {
  assert.match(source, /administrator:\s*'\/admin\/animal-settings'/);
  assert.match(source, /DEFAULT_PORTAL = '\/portal'/);
  assert.match(source, /owner:\s*'\/admin\/animal-settings'/);
});

test('platform_admin maps to farm-settings and not the farm portal', () => {
  assert.match(source, /platform_admin:\s*'\/admin\/farm-settings'/);
  assert.doesNotMatch(source, /manager:\s*'\/admin/);
});

test('farm_owner group maps to /farm', () => {
  assert.match(source, /farm_owner:\s*'\/farm'/);
  assert.match(source, /function isFarmPortal/);
});
