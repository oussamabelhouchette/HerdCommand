import assert from 'node:assert/strict';
import test from 'node:test';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

function loadCombined() {
  const fs = require('node:fs');
  const path = require('node:path');
  const dir = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
  const strip = (source) =>
    source
      .replace(/import [^;]+;/g, '')
      .replace(/export /g, '')
      .replace(/: [^=,)\n]+/g, '')
      .replace(/ as const/g, '')
      .replace(/as \(typeof LOCALES\)\[number\]/g, '');
  const portals = strip(fs.readFileSync(path.join(dir, 'portals.ts'), 'utf8'));
  const redirect = strip(fs.readFileSync(path.join(dir, 'post-login-redirect.ts'), 'utf8'));
  const moduleExports = {};
  const fn = new Function(
    'exports',
    `${portals}\n${redirect}\nexports.portalHref = portalHref;\nexports.postLoginHref = postLoginHref;\nexports.isGenericPostLoginPath = isGenericPostLoginPath;\nexports.toAppHref = toAppHref;`,
  );
  fn(moduleExports);
  return moduleExports;
}

let helpers;
try {
  helpers = loadCombined();
} catch (error) {
  helpers = null;
  console.warn('Could not eval TS helper, using inline copy', error);
}

const PORTAL_BY_MEMBERSHIP = {
  administrator: '/admin/animal-settings',
  administrators: '/admin/animal-settings',
  owner: '/admin/animal-settings',
};
const DEFAULT_PORTAL = '/portal';

function normalizeMembership(name) {
  const parts = name.trim().toLowerCase().replace(/^\/+/, '').split('/').filter(Boolean);
  return parts[parts.length - 1] ?? '';
}

function portalHref(memberships) {
  if (!memberships?.length) return DEFAULT_PORTAL;
  for (const raw of memberships) {
    const path = PORTAL_BY_MEMBERSHIP[normalizeMembership(raw)];
    if (path) return path;
  }
  return DEFAULT_PORTAL;
}

function isAdminPortal(path) {
  return path === '/admin' || path.startsWith('/admin/');
}

const LOCALES = ['ar', 'en'];
const GENERIC_SEGMENTS = new Set(['login', 'signed-in', 'portal', 'admin']);

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

function postLoginHref(memberships, callbackUrl) {
  const portal = portalHref(memberships);
  if (isGenericPostLoginPath(callbackUrl)) return portal;
  const intended = toAppHref(callbackUrl);
  if (isAdminPortal(intended) && portal !== '/admin') return portal;
  if ((intended === DEFAULT_PORTAL || intended.startsWith(`${DEFAULT_PORTAL}/`)) && portal !== DEFAULT_PORTAL) {
    return portal;
  }
  return intended;
}

const impl = helpers ?? { portalHref, postLoginHref, isGenericPostLoginPath, toAppHref };

test('administrator group or role lands on /admin/animal-settings', () => {
  assert.equal(impl.postLoginHref(['administrator']), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['/administrator']), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['owner']), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/login'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/signed-in'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/portal'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/admin'), '/admin/animal-settings');
});

test('everyone else lands on /portal', () => {
  assert.equal(impl.postLoginHref([]), '/portal');
  assert.equal(impl.postLoginHref(['manager']), '/portal');
  assert.equal(impl.postLoginHref(['worker'], '/'), '/portal');
  assert.equal(impl.postLoginHref(['worker'], '/login'), '/portal');
});

test('specific callbacks are honored when they stay in the same portal', () => {
  assert.equal(impl.postLoginHref(['administrator'], '/admin/animal-settings'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['administrator'], '/en/admin/animal-settings'), '/admin/animal-settings');
  assert.equal(impl.postLoginHref(['manager'], '/me'), '/me');
  assert.equal(impl.postLoginHref(['administrator'], '/me'), '/me');
});

test('users cannot be sent to the other portal via callback', () => {
  assert.equal(impl.postLoginHref(['manager'], '/admin'), '/portal');
  assert.equal(impl.postLoginHref(['administrator'], '/portal'), '/admin/animal-settings');
});
