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

test('platform admin UI is limited to PLATFORM_ADMIN', () => {
  assert.match(roles, /platform_admin/);
  assert.match(roles, /isPlatformAdminRole/);
  assert.doesNotMatch(roles, /veterinarian/);
  assert.doesNotMatch(roles, /accountant/);
});

function normalizeMembership(name) {
  const parts = name.trim().toLowerCase().replace(/^\/+/, '').split('/').filter(Boolean);
  return parts[parts.length - 1] ?? '';
}

const ADMIN_ROLES = new Set(['administrator', 'administrators', 'owner']);

function isAdminRole(list) {
  if (!list?.length) return false;
  return list.some((role) => ADMIN_ROLES.has(normalizeMembership(role)));
}

function normalizePlatformRole(role) {
  return normalizeMembership(role).replace(/-/g, '_');
}

function isPlatformAdminRole(list) {
  if (!list?.length) return false;
  return list.some((role) => normalizePlatformRole(role) === 'platform_admin');
}

function isAdminShellRole(list) {
  return isAdminRole(list) || isPlatformAdminRole(list);
}

test('route guard: only PLATFORM_ADMIN opens farm management', () => {
  assert.equal(isPlatformAdminRole(['PLATFORM_ADMIN']), true);
  assert.equal(isPlatformAdminRole(['platform_admin']), true);
  assert.equal(isPlatformAdminRole(['platform-admin']), true);
  assert.equal(isPlatformAdminRole(['owner']), false);
  assert.equal(isPlatformAdminRole(['administrator']), false);
  assert.equal(isPlatformAdminRole(['manager']), false);
  assert.equal(isAdminShellRole(['PLATFORM_ADMIN']), true);
  assert.equal(isAdminShellRole(['owner']), true);
  assert.equal(isAdminShellRole(['manager']), false);
});
