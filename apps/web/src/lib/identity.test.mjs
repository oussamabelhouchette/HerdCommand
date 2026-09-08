import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'identity.ts'), 'utf8');
const ui = readFileSync(join(dir, '../components/admin/OwnerLookup.tsx'), 'utf8');
const settings = readFileSync(join(dir, '../components/admin/FarmSettings.tsx'), 'utf8');

test('identity client calls Spring lookup and never Keycloak admin', () => {
  assert.match(client, /\/api\/v1\/platform\/identity\/lookup/);
  assert.match(client, /function lookupIdentity/);
  assert.match(client, /function normalizeEmail/);
  assert.match(client, /function ownerLookupStatus/);
  assert.match(client, /invitationRequired/);
  assert.doesNotMatch(client, /admin\/realms/);
  assert.doesNotMatch(client, /admin-cli/);
  assert.doesNotMatch(client, /client_secret/);
  assert.doesNotMatch(client, /KEYCLOAK_ADMIN/);
});

test('owner lookup UI covers found, invitation, disabled, loading and error', () => {
  assert.match(ui, /DEBOUNCE_MS = 400/);
  assert.match(ui, /lookupIdentity/);
  assert.match(ui, /ownerLookupStatus/);
  assert.match(ui, /status === 'found'/);
  assert.match(ui, /status === 'invitation'/);
  assert.match(ui, /status === 'disabled'/);
  assert.match(ui, /status === 'loading'/);
  assert.match(ui, /status === 'error'/);
  assert.match(ui, /t\('ownerInvite'\)/);
  assert.doesNotMatch(ui, /admin\/realms/);
  assert.doesNotMatch(ui, /client_secret/);
});

test('farm settings hosts owner lookup without assigning Keycloak farm roles', () => {
  assert.match(settings, /OwnerLookup/);
  assert.doesNotMatch(settings, /FARM_OWNER/);
  assert.doesNotMatch(settings, /realmRoles/);
});
