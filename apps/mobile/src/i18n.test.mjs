import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const source = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'i18n.ts'), 'utf8');

test('mobile defaults to Arabic', () => {
  assert.match(source, /defaultLocale = 'ar'/);
  assert.match(source, /i18n\.locale = 'ar'/);
});
