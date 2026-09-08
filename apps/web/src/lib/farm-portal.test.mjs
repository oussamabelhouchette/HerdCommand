import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const portal = readFileSync(join(dir, 'farm-portal.ts'), 'utf8');
const farms = readFileSync(join(dir, 'owner-farms.ts'), 'utf8');
const farmLayout = readFileSync(join(dir, '../app/[locale]/farm/layout.tsx'), 'utf8');
const farmIndex = readFileSync(join(dir, '../app/[locale]/farm/page.tsx'), 'utf8');
const farmIdLayout = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/layout.tsx'), 'utf8');
const dashboard = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/page.tsx'), 'utf8');
const animals = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/animals/page.tsx'), 'utf8');
const missing = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/not-found.tsx'), 'utf8');

test('farm portal loads owner farms once and resolves the URL farm exactly', () => {
  assert.match(portal, /loadFarmPortal = cache/);
  assert.match(portal, /requireCurrentFarm = cache/);
  assert.match(portal, /findOwnerFarm/);
  assert.match(farms, /function findOwnerFarm/);
  assert.match(farms, /function firstOwnerFarm/);
  assert.doesNotMatch(farms, /selectOwnerFarm/);
  assert.doesNotMatch(portal, /\/api\/v1\/platform\/farms/);
});

test('farm layouts own auth and farm identity so pages stay thin', () => {
  assert.match(farmLayout, /loadFarmPortal/);
  assert.match(farmIndex, /firstOwnerFarm/);
  assert.match(farmIndex, /redirect/);
  assert.match(farmIdLayout, /requireCurrentFarm/);
  assert.match(farmIdLayout, /notFound\(/);
  assert.match(missing, /notFoundTitle/);
  assert.doesNotMatch(dashboard, /redirect/);
  assert.doesNotMatch(dashboard, /loadOwnerFarms/);
  assert.doesNotMatch(animals, /redirect/);
  assert.doesNotMatch(animals, /loadOwnerFarms/);
});

function findOwnerFarm(list, farmId) {
  if (!farmId) return undefined;
  return list.find((farm) => farm.id === farmId);
}

function firstOwnerFarm(list) {
  return list[0];
}

test('unknown farm ids are not silently replaced with another farm', () => {
  const list = [{ id: '1' }, { id: '2' }];
  assert.equal(findOwnerFarm(list, '2')?.id, '2');
  assert.equal(findOwnerFarm(list, 'missing'), undefined);
  assert.equal(findOwnerFarm([], '1'), undefined);
  assert.equal(firstOwnerFarm(list)?.id, '1');
});
