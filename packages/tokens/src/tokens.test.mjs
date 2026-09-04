import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const css = readFileSync(join(dir, 'tokens.css'), 'utf8');
const ts = readFileSync(join(dir, 'index.ts'), 'utf8');

test('CSS tokens include blueprint palette', () => {
  assert.match(css, /#12372b/i);
  assert.match(css, /#fbf9f4/i);
  assert.match(css, /#c89b5a/i);
  assert.match(css, /--hc-touch-min: 44px/);
});

test('TypeScript tokens include semantic and raw palette split', () => {
  assert.match(ts, /primary900: '#12372B'/);
  assert.match(ts, /export const semantic/);
  assert.match(ts, /minTouchTarget: 44/);
});
