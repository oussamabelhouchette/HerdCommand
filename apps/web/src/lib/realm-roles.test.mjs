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

function realmRolesFromAccessToken(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return [];
  const realmAccess = payload.realm_access;
  if (!realmAccess || typeof realmAccess !== 'object' || Array.isArray(realmAccess)) return [];
  const roles = realmAccess.roles;
  if (!Array.isArray(roles)) return [];
  return roles.filter((role) => typeof role === 'string' && role.trim().length > 0);
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
