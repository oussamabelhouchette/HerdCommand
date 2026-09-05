import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const source = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'platform.ts'), 'utf8');

test('platform client only calls /api/v1/platform paths', () => {
  assert.match(source, /\/api\/v1\/platform/);
  assert.doesNotMatch(source, /probePlatformFarms/);
  assert.doesNotMatch(source, /keycloak/i);
  assert.doesNotMatch(source, /admin-cli/);
});
