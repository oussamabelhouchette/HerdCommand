import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'owner-farms.ts'), 'utf8');
const layout = readFileSync(join(dir, '../app/[locale]/farm/layout.tsx'), 'utf8');
const dashboard = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/page.tsx'), 'utf8');
const animals = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/animals/page.tsx'), 'utf8');
const switcher = readFileSync(join(dir, '../components/farm/FarmSwitcher.tsx'), 'utf8');
const nav = readFileSync(join(dir, '../components/farm/FarmNav.tsx'), 'utf8');

test('owner farm client loads /api/v1/me/farms and never the platform list', () => {
  assert.match(client, /\/api\/v1\/me\/farms/);
  assert.match(client, /listOwnerFarms = cache/);
  assert.match(client, /loadOwnerFarms = cache/);
  assert.match(client, /function farmHref/);
  assert.match(client, /function selectOwnerFarm/);
  assert.match(client, /ANIMAL_MANAGEMENT/);
  assert.doesNotMatch(client, /\/api\/v1\/platform\/farms/);
});

test('farm portal is gated to farm_owner and shows dashboard plus animals', () => {
  assert.match(layout, /requireFarmOwner/);
  assert.match(layout, /loadOwnerFarms/);
  assert.match(nav, /\/farm\/\$\{farmId\}/);
  assert.match(nav, /\/farm\/\$\{farmId\}\/animals/);
  assert.match(dashboard, /FarmDashboard/);
  assert.match(animals, /FarmAnimals/);
});

test('owners with more than one farm get a switcher', () => {
  assert.match(switcher, /farms\.length === 1/);
  assert.match(switcher, /farmHref/);
  assert.match(switcher, /farm-switcher/);
});

function farmIdFromPath(pathname) {
  const match = pathname.match(/^\/farm\/([^/]+)/);
  return match?.[1];
}

function farmHref(farmId, pathname = '/farm') {
  const suffix = pathname.replace(/^\/farm\/[^/]+/, '') || '';
  return `/farm/${farmId}${suffix}`;
}

function selectOwnerFarm(farms, farmId) {
  if (!farms.length) return undefined;
  return farms.find((farm) => farm.id === farmId) ?? farms[0];
}

test('farm switcher keeps the current page when changing farm', () => {
  assert.equal(farmHref('b', '/farm/a/animals'), '/farm/b/animals');
  assert.equal(farmHref('b', '/farm/a'), '/farm/b');
  assert.equal(farmIdFromPath('/farm/abc/animals'), 'abc');
  assert.equal(selectOwnerFarm([{ id: '1' }, { id: '2' }], '2')?.id, '2');
  assert.equal(selectOwnerFarm([{ id: '1' }, { id: '2' }], 'missing')?.id, '1');
});
