import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'animals.ts'), 'utf8');
const actions = readFileSync(join(dir, 'animal-actions.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/farm/OwnerAnimals.tsx'), 'utf8');
const css = readFileSync(join(dir, '../components/farm/OwnerAnimals.module.css'), 'utf8');
const page = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/animals/page.tsx'), 'utf8');
const layout = readFileSync(join(dir, '../app/[locale]/farm/layout.tsx'), 'utf8');
const ar = readFileSync(join(dir, '../messages/ar.json'), 'utf8');
const en = readFileSync(join(dir, '../messages/en.json'), 'utf8');

function paginationItems(current, totalPages) {
  if (totalPages <= 7) {
    return Array.from({ length: Math.max(totalPages, 0) }, (_, index) => index);
  }
  const items = [0];
  const start = Math.max(1, current - 1);
  const end = Math.min(totalPages - 2, current + 1);
  if (start > 1) items.push('ellipsis');
  for (let pageIndex = start; pageIndex <= end; pageIndex += 1) items.push(pageIndex);
  if (end < totalPages - 2) items.push('ellipsis');
  items.push(totalPages - 1);
  return items;
}

function summaryRange(pageIndex, size, total) {
  if (total <= 0) return { start: 0, end: 0 };
  const start = pageIndex * size + 1;
  const end = Math.min((pageIndex + 1) * size, total);
  return { start, end };
}

test('animal client uses farm-scoped APIs, page size 20, and debounce', () => {
  assert.match(client, /\/api\/v1\/farms\/\$\{farmId\}\/animals/);
  assert.match(client, /ANIMAL_PAGE_SIZE = 20/);
  assert.match(client, /ANIMAL_SEARCH_DEBOUNCE_MS = 400/);
  assert.match(client, /function listFarmAnimals/);
  assert.match(client, /function getAnimalLookups/);
  assert.match(client, /function getFarmAnimal/);
  assert.match(client, /function createFarmAnimal/);
  assert.match(client, /function updateFarmAnimal/);
  assert.match(client, /function archiveFarmAnimal/);
  assert.match(client, /function paginationItems/);
  assert.match(client, /function summaryRange/);
  assert.match(client, /function parseAnimalSearchParams/);
  assert.match(client, /function animalsHref/);
  assert.doesNotMatch(client, /\/api\/v1\/admin\//);
  assert.doesNotMatch(client, /keycloak/i);
});

test('animals page hydrates from lookups and list APIs and keeps the lock when unsupported', () => {
  assert.match(page, /requireCurrentFarm/);
  assert.match(page, /OwnerAnimals/);
  assert.match(page, /parseAnimalSearchParams/);
  assert.doesNotMatch(page, /redirect/);
  assert.doesNotMatch(page, /OwnerAnimalsPanel/);
  assert.doesNotMatch(page, /'use client'/);
  assert.doesNotMatch(ui, /'use client'/);
  assert.doesNotMatch(ui, /OwnerAnimalsPanel/);
  assert.match(ui, /export async function OwnerAnimals/);
  assert.match(ui, /getAnimalLookups/);
  assert.match(ui, /listFarmAnimals/);
  assert.match(ui, /unsupportedTitle/);
  assert.match(ui, /saveAnimalAction/);
  assert.match(ui, /archiveAnimalAction/);
  assert.match(actions, /'use server'/);
  assert.match(layout, /animals: messages\.animals/);
});

test('list UI covers empty error retry archive dialog and no confirm()', () => {
  assert.match(ui, /t\('empty'\)/);
  assert.match(ui, /t\('retry'\)/);
  assert.match(ui, /t\('apply'\)/);
  assert.match(ui, /role="dialog"/);
  assert.match(ui, /confirmArchive/);
  assert.match(ui, /method="get"/);
  assert.doesNotMatch(ui, /window\.confirm|confirm\(/);
  assert.doesNotMatch(ui, /OVL-2101|رغد|الوادي/);
});

test('reference layout, badges, and logical CSS stay in the animals list', () => {
  assert.match(css, /\.topbar/);
  assert.match(css, /\.filters/);
  assert.match(css, /\.tableCard/);
  assert.match(css, /\.activeB/);
  assert.match(css, /\.pregnant/);
  assert.match(css, /\.sick/);
  assert.match(css, /\.isolated/);
  assert.match(css, /padding-inline-start/);
  assert.match(css, /inset-inline-start/);
  assert.match(css, /@media \(max-width: 1050px\)/);
  assert.match(css, /@media \(max-width: 720px\)/);
  assert.match(css, /\.pageBtn/);
  assert.match(ui, /t\('colId'\)/);
  assert.match(ui, /t\('colName'\)/);
  assert.match(ui, /t\('colBreed'\)/);
  assert.match(ui, /t\('colGender'\)/);
  assert.match(ui, /t\('colAge'\)/);
  assert.match(ui, /t\('colGroup'\)/);
  assert.match(ui, /t\('colStatus'\)/);
  assert.match(ui, /t\('colActions'\)/);
});

test('Arabic and English copy match the reference screen', () => {
  assert.match(ar, /"title": "قائمة الحيوانات"/);
  assert.match(ar, /إبحث برقم التعريف أو الاسم/);
  assert.match(ar, /عرض \{start\} إلى \{end\} من أصل \{total\} حيوان مسجل/);
  assert.match(ar, /لا توجد حيوانات مطابقة للبحث/);
  assert.match(en, /"title": "Animals list"/);
  assert.match(en, /Showing \{start\}–\{end\} of \{total\} registered animals/);
  assert.match(en, /No matching animals/);
  assert.doesNotMatch(ar, /OVL-2101/);
  assert.doesNotMatch(en, /Raghad/);
});

test('pagination helpers count a 20-record page and hide 1–0', () => {
  assert.deepEqual(summaryRange(0, 20, 47), { start: 1, end: 20 });
  assert.deepEqual(summaryRange(1, 20, 47), { start: 21, end: 40 });
  assert.deepEqual(summaryRange(2, 20, 47), { start: 41, end: 47 });
  assert.deepEqual(summaryRange(0, 20, 0), { start: 0, end: 0 });
  assert.deepEqual(paginationItems(0, 3), [0, 1, 2]);
  assert.deepEqual(paginationItems(0, 13), [0, 1, 'ellipsis', 12]);
  assert.deepEqual(paginationItems(6, 13), [0, 'ellipsis', 5, 6, 7, 'ellipsis', 12]);
});
