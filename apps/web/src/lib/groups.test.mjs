import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const groups = readFileSync(join(dir, 'groups.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/GroupManagement.tsx'), 'utf8');
const settings = readFileSync(join(dir, '../components/admin/AnimalSettings.tsx'), 'utf8');

test('group client includes farmId in every path and has no assign or delete helper', () => {
  assert.match(groups, /`\/api\/v1\/farms\/\$\{farmId\}\/animal-groups`/);
  assert.match(groups, /`\/api\/v1\/farms\/\$\{farmId\}\/animal-groups\/\$\{groupId\}`/);
  assert.match(groups, /function listFarms/);
  assert.match(groups, /function createGroup/);
  assert.match(groups, /function updateGroup/);
  assert.match(groups, /function updateGroupActive/);
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
  assert.doesNotMatch(ui, /group-type|filterAllGroupTypes|colGroupType|GROUP_TYPE_CODES/);
});
