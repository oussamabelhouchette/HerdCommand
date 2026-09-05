import assert from 'node:assert/strict';
import test from 'node:test';

function decodeJwtPayload(token) {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const json = Buffer.from(parts[1], 'base64url').toString('utf8');
    const payload = JSON.parse(json);
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return null;
    return payload;
  } catch {
    return null;
  }
}

function stringList(value) {
  if (!Array.isArray(value)) return [];
  return value.filter((item) => typeof item === 'string' && item.trim().length > 0);
}

function realmRolesFromAccessToken(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return [];
  const realmAccess = payload.realm_access;
  if (!realmAccess || typeof realmAccess !== 'object' || Array.isArray(realmAccess)) return [];
  return stringList(realmAccess.roles);
}

function groupsFromAccessToken(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return [];
  return stringList(payload.groups);
}

function membershipsFromAccessToken(accessToken) {
  return [...new Set([...realmRolesFromAccessToken(accessToken), ...groupsFromAccessToken(accessToken)])];
}

function fakeAccessToken(payload) {
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `header.${body}.sig`;
}

test('reads Keycloak realm_access.roles from the access token', () => {
  const token = fakeAccessToken({
    realm_access: { roles: ['owner', 'offline_access'] },
  });
  assert.deepEqual(realmRolesFromAccessToken(token), ['owner', 'offline_access']);
});

test('returns empty roles when realm_access is missing', () => {
  const token = fakeAccessToken({ preferred_username: 'worker' });
  assert.deepEqual(realmRolesFromAccessToken(token), []);
});

test('reads Keycloak groups claim from the access token', () => {
  const token = fakeAccessToken({
    groups: ['/administrator', 'farm-workers'],
    realm_access: { roles: ['offline_access'] },
  });
  assert.deepEqual(groupsFromAccessToken(token), ['/administrator', 'farm-workers']);
  assert.deepEqual(membershipsFromAccessToken(token), ['offline_access', '/administrator', 'farm-workers']);
});
