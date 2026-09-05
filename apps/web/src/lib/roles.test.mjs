import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const roles = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'roles.ts'), 'utf8');

test('admin UI is limited to owner and administrator', () => {
  assert.match(roles, /administrator/);
  assert.match(roles, /owner/);
  assert.doesNotMatch(roles, /manager/);
});
