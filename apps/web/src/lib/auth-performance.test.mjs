import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));

function loadSessionCookie() {
  const source = readFileSync(join(dir, 'session-cookie.ts'), 'utf8')
    .replace(/export /g, '')
    .replace(/: string\[\]/g, '');
  const module = { exports: {} };
  eval(`${source}; module.exports.hasAuthJsSessionCookie = hasAuthJsSessionCookie`);
  return module.exports;
}

test('guest requests skip Auth.js session work without a cookie', () => {
  const { hasAuthJsSessionCookie } = loadSessionCookie();
  assert.equal(hasAuthJsSessionCookie([]), false);
  assert.equal(hasAuthJsSessionCookie(['HERDCOMMAND_LOCALE']), false);
  assert.equal(hasAuthJsSessionCookie(['authjs.session-token']), true);
  assert.equal(hasAuthJsSessionCookie(['authjs.session-token.0']), true);
  assert.equal(hasAuthJsSessionCookie(['__Secure-authjs.session-token.1']), true);

  const session = readFileSync(join(dir, 'session.ts'), 'utf8');
  assert.match(session, /hasAuthJsSessionCookie/);
  assert.match(session, /return null/);
});

test('login and other public pages do not run Auth.js in middleware', () => {
  const middleware = readFileSync(join(dir, '../middleware.ts'), 'utf8');
  assert.doesNotMatch(middleware, /export default auth\(/);
  assert.match(middleware, /isProtectedPath/);
  assert.match(middleware, /return protect\(request\)/);
});

test('logout clears the app cookie without reading or refreshing the session', () => {
  const logout = readFileSync(join(dir, 'logout.ts'), 'utf8');
  const federated = readFileSync(join(dir, '../app/api/auth/federated-logout/route.ts'), 'utf8');
  assert.doesNotMatch(logout, /await auth\(/);
  assert.match(logout, /signOut/);
  assert.match(logout, /idToken/);
  assert.doesNotMatch(federated, /await auth\(/);
  assert.match(federated, /signOut/);
});

test('login page is static and does not read the session on the server', () => {
  const login = readFileSync(join(dir, '../app/[locale]/(auth)/login/page.tsx'), 'utf8');
  const config = readFileSync(join(dir, '../../next.config.mjs'), 'utf8');
  const signIn = readFileSync(join(dir, '../components/SignInButton.tsx'), 'utf8');
  assert.match(login, /export const dynamic = 'force-static'/);
  assert.match(login, /export const revalidate = 300/);
  assert.doesNotMatch(login, /getSession/);
  assert.doesNotMatch(login, /searchParams/);
  assert.match(login, /RedirectIfSignedIn/);
  assert.match(signIn, /continuePath/);
  assert.match(config, /max-age=31536000, immutable/);
  assert.match(config, /stale-while-revalidate=300/);
});
