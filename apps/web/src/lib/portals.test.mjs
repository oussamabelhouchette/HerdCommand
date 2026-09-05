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
