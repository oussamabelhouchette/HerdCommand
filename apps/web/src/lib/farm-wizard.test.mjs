import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const client = readFileSync(join(dir, 'farms.ts'), 'utf8');
const wizard = readFileSync(join(dir, '../components/admin/FarmCreateWizard.tsx'), 'utf8');
const settings = readFileSync(join(dir, '../components/admin/FarmSettings.tsx'), 'utf8');
const api = readFileSync(join(dir, 'api.ts'), 'utf8');

test('create client posts one farm with a reusable idempotency key', () => {
  assert.match(client, /function createPlatformFarm/);
  assert.match(client, /method: 'POST'/);
  assert.match(client, /'Idempotency-Key': idempotencyKey/);
  assert.match(client, /function newIdempotencyKey/);
  assert.match(client, /crypto\.randomUUID/);
  assert.match(api, /headers\?: Record<string, string>/);
  assert.doesNotMatch(wizard, /code: form/);
});

test('wizard keeps four steps, owner lookup states, and catalog checkboxes', () => {
  assert.match(wizard, /data-wizard-step=\{step\}/);
  assert.match(wizard, /setStep\(\(current\) => \(current < 4/);
  assert.match(wizard, /lookupIdentity/);
  assert.match(wizard, /ownerStatus === 'found'/);
  assert.match(wizard, /ownerStatus === 'invitation'/);
  assert.match(wizard, /ownerInvite/);
  assert.match(wizard, /selectedFeatureCodes\(features\)/);
  assert.match(wizard, /features\.map/);
  assert.match(wizard, /disabled=\{disabled\}/);
  assert.match(wizard, /!feature\.enableable/);
  assert.match(wizard, /data-wizard-review/);
  assert.doesNotMatch(wizard, /id="wizard-code"/);
  assert.doesNotMatch(wizard, /farmCode/);
  assert.doesNotMatch(wizard, /@tanstack\/query/);
  assert.doesNotMatch(wizard, /react-hook-form/);
});

test('failed create maps field errors to a step and reuses the same idempotency key', () => {
  assert.match(client, /function wizardStepForField/);
  assert.match(client, /field.startsWith\('owner'\)/);
  assert.match(client, /field.startsWith\('subscription'\)/);
  assert.match(wizard, /wizardStepForErrors\(mapped\)/);
  assert.match(wizard, /idempotencyKey\.current/);
  assert.match(wizard, /payloadFingerprint/);
  assert.match(wizard, /disabled=\{saving\}/);
  assert.match(wizard, /createPlatformFarm\(token, locale, body, idempotencyKey\.current\)/);
});

test('farm settings opens the wizard and shows the created farm', () => {
  assert.match(settings, /FarmCreateWizard/);
  assert.match(settings, /setWizardOpen\(true\)/);
  assert.match(settings, /setCreatedId\(created\.id\)/);
  assert.match(settings, /setDetailsId\(created\.id\)/);
  assert.match(settings, /void reload\(\)/);
  assert.doesNotMatch(settings, /wizardLater/);
});
