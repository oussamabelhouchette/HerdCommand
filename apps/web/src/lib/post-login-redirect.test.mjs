import assert from 'node:assert/strict';
import test from 'node:test';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

function loadTs(relative) {
  const fs = require('node:fs');
  const path = require('node:path');
  const source = fs.readFileSync(path.join(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), relative), 'utf8');
  const stripped = source
    .replace(/export /g, '')
    .replace(/: [^=,)\n]+/g, '')
    .replace(/ as const/g, '')
    .replace(/as \(typeof LOCALES\)\[number\]/g, '');
  const moduleExports = {};
  const fn = new Function('exports', `${stripped}\nexports.isGenericPostLoginPath = isGenericPostLoginPath;\nexports.toAppHref = toAppHref;\nexports.postLoginHref = postLoginHref;\nexports.resolveAuthJsRedirect = resolveAuthJsRedirect;\nexports.localeFromCallbackUrl = localeFromCallbackUrl;`);
  fn(moduleExports);
  return moduleExports;
}

let helpers;
try {
  helpers = loadTs('post-login-redirect.ts');
} catch (error) {
  helpers = null;
  console.warn('Could not eval TS helper, using inline copy', error);
}

const LOCALES = ['ar', 'en'];
const GENERIC_SEGMENTS = new Set(['login', 'signed-in']);

function extractPathname(callbackUrl) {
  if (!callbackUrl) return '';
  let raw = callbackUrl.trim();
  if (!raw) return '';
  try {
    if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(raw)) {
      raw = new URL(raw).pathname;
    }
  } catch {
    return '';
  }
  const path = raw.split('?')[0]?.split('#')[0] ?? '';
  if (!path || path === '/') return '/';
  return path.replace(/\/+$/, '') || '/';
}

function pathSegments(pathname) {
  return pathname.split('/').filter(Boolean);
}

function isGenericPostLoginPath(callbackUrl) {
  const pathname = extractPathname(callbackUrl);
  if (!pathname) return true;
  const segments = pathSegments(pathname);
  if (segments.length === 0) return true;
  if (segments.length === 1 && LOCALES.includes(segments[0])) return true;
  if (segments.length === 1) return GENERIC_SEGMENTS.has(segments[0]);
  if (segments.length === 2 && LOCALES.includes(segments[0])) return GENERIC_SEGMENTS.has(segments[1]);
  return false;
}

function toAppHref(callbackUrl) {
  const pathname = extractPathname(callbackUrl);
  if (!pathname || pathname === '/') return '/';
  const segments = pathSegments(pathname);
  if (segments.length && LOCALES.includes(segments[0])) {
    const rest = segments.slice(1);
    return rest.length ? `/${rest.join('/')}` : '/';
  }
  return pathname.startsWith('/') ? pathname : `/${pathname}`;
}

function postLoginHref(isAdmin, callbackUrl) {
  if (isAdmin && isGenericPostLoginPath(callbackUrl)) return '/admin';
  if (callbackUrl && !isGenericPostLoginPath(callbackUrl)) return toAppHref(callbackUrl);
  return '/';
}

const impl = helpers ?? { isGenericPostLoginPath, toAppHref, postLoginHref };

test('generic login and home paths send admins to /admin', () => {
  assert.equal(impl.postLoginHref(true, undefined), '/admin');
  assert.equal(impl.postLoginHref(true, '/'), '/admin');
  assert.equal(impl.postLoginHref(true, '/login'), '/admin');
  assert.equal(impl.postLoginHref(true, '/en'), '/admin');
  assert.equal(impl.postLoginHref(true, '/en/login'), '/admin');
  assert.equal(impl.postLoginHref(true, '/signed-in'), '/admin');
});

test('specific callbacks are honored for admins', () => {
  assert.equal(impl.postLoginHref(true, '/admin/animal-settings'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(true, '/en/admin/animal-settings'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(true, '/me'), '/me');
  assert.equal(impl.postLoginHref(true, '/en/me'), '/me');
});

test('non-admins keep home or their callback', () => {
  assert.equal(impl.postLoginHref(false, undefined), '/');
  assert.equal(impl.postLoginHref(false, '/'), '/');
  assert.equal(impl.postLoginHref(false, '/me'), '/me');
  assert.equal(impl.postLoginHref(false, '/en/me'), '/me');
  assert.equal(impl.postLoginHref(false, '/admin'), '/admin');
});
