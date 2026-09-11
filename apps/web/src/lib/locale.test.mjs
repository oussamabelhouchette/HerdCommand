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

function prefixedHref(href, locale) {
  const safeLocale = ['ar', 'en'].includes(locale) ? locale : 'ar';
  const queryIndex = href.indexOf('?');
  const path = queryIndex === -1 ? href : href.slice(0, queryIndex);
  const search = queryIndex === -1 ? '' : href.slice(queryIndex);
  const normalized = path.startsWith('/') ? path : `/${path}`;
  if (normalized === `/${safeLocale}` || normalized.startsWith(`/${safeLocale}/`)) {
    return `${normalized}${search}`;
  }
  return `/${safeLocale}${normalized}${search}`;
}

test('server action redirects always include the locale prefix', () => {
  assert.equal(prefixedHref('/farm/1/animals', 'ar'), '/ar/farm/1/animals');
  assert.equal(prefixedHref('/farm/1/animals?error=save', 'en'), '/en/farm/1/animals?error=save');
  assert.equal(prefixedHref('/login', 'ar'), '/ar/login');
  assert.equal(prefixedHref('/ar/farm/1/groups', 'ar'), '/ar/farm/1/groups');
  const helper = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'locale-path.ts'), 'utf8');
  const animals = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'animal-actions.ts'), 'utf8');
  const groups = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'group-actions.ts'), 'utf8');
  assert.match(helper, /function prefixedHref/);
  assert.match(animals, /prefixedHref\(animalsHref/);
  assert.match(groups, /prefixedHref\(groupsHref/);
  assert.match(animals, /from 'next\/navigation'/);
  assert.match(groups, /from 'next\/navigation'/);
  assert.doesNotMatch(animals, /from '@\/i18n\/navigation'/);
  assert.doesNotMatch(groups, /from '@\/i18n\/navigation'/);
});
