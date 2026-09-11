import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const groups = readFileSync(join(dir, 'groups.ts'), 'utf8');
const actions = readFileSync(join(dir, 'group-actions.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/GroupManagement.tsx'), 'utf8');
const farmUi = readFileSync(join(dir, '../components/farm/OwnerGroups.tsx'), 'utf8');
const page = readFileSync(join(dir, '../app/[locale]/farm/[farmId]/groups/page.tsx'), 'utf8');
const settings = readFileSync(join(dir, '../components/admin/AnimalSettings.tsx'), 'utf8');
const ar = readFileSync(join(dir, '../messages/ar.json'), 'utf8');
const en = readFileSync(join(dir, '../messages/en.json'), 'utf8');

test('group client includes farmId in every path and has no assign or delete helper', () => {
  assert.match(groups, /`\/api\/v1\/farms\/\$\{farmId\}\/animal-groups`/);
  assert.match(groups, /`\/api\/v1\/farms\/\$\{farmId\}\/animal-groups\/\$\{groupId\}`/);
  assert.match(groups, /function listFarms/);
  assert.match(groups, /function createGroup/);
  assert.match(groups, /function updateGroup/);
  assert.match(groups, /function updateGroupActive/);
  assert.match(groups, /function getGroup/);
  assert.match(groups, /function parseGroupSearchParams/);
  assert.match(groups, /function groupsHref/);
  assert.doesNotMatch(groups, /function assign/);
  assert.doesNotMatch(groups, /\/animals`/);
  assert.doesNotMatch(groups, /method: 'DELETE'/);
});

test('groups tab is wired and can create like breeds', () => {
  assert.match(settings, /GroupManagement/);
  assert.match(settings, /setTab\('groups'\)/);
  assert.match(ui, /createGroup\(token, locale, farmId/);
  assert.match(ui, /addGroup/);
  assert.match(ui, /GROUP_CODE_ALREADY_EXISTS/);
});

test('edit keeps the group code out of the update body', () => {
  assert.match(ui, /disabled=\{Boolean\(editing\)\}/);
  assert.match(ui, /updateGroup\(token, locale, farmId, editing\.id, payload\)/);
  assert.doesNotMatch(ui, /updateGroup\([^)]*code:/);
});

test('group type is hidden in the UI and saved as the default OTHER', () => {
  assert.match(groups, /DEFAULT_GROUP_TYPE: GroupTypeCode = 'OTHER'/);
  assert.match(ui, /groupTypeCode: DEFAULT_GROUP_TYPE/);
  assert.match(actions, /groupTypeCode: DEFAULT_GROUP_TYPE/);
  assert.doesNotMatch(ui, /group-type|filterAllGroupTypes|colGroupType|GROUP_TYPE_CODES/);
  assert.doesNotMatch(farmUi, /group-type|filterAllGroupTypes|colGroupType|GROUP_TYPE_CODES/);
});

test('farm groups page is a server screen with no client island import', () => {
  assert.match(page, /requireCurrentFarm/);
  assert.match(page, /OwnerGroups/);
  assert.match(page, /parseGroupSearchParams/);
  assert.doesNotMatch(page, /redirect/);
  assert.doesNotMatch(page, /'use client'/);
  assert.doesNotMatch(farmUi, /'use client'/);
  assert.match(farmUi, /export async function OwnerGroups/);
  assert.match(farmUi, /saveGroupAction/);
  assert.match(farmUi, /setGroupActiveAction/);
  assert.match(farmUi, /method="get"/);
  assert.doesNotMatch(farmUi, /window\.confirm|confirm\(/);
  assert.match(actions, /'use server'/);
});

test('Arabic and English group copy exist for the farm screen', () => {
  assert.match(ar, /"title": "المجموعات"/);
  assert.match(ar, /لا توجد مجموعات مطابقة للبحث/);
  assert.match(en, /"title": "Groups"/);
  assert.match(en, /No matching groups/);
});
