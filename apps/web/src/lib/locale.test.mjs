import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const routing = readFileSync(
  join(dirname(fileURLToPath(import.meta.url)), '../i18n/routing.ts'),
  'utf8',
);

test('Arabic is the default locale', () => {
  assert.match(routing, /defaultLocale: 'ar'/);
  assert.match(routing, /locales: \['ar', 'en'\]/);
});
