const LOCALES = ['ar', 'en'] as const;
const GENERIC_SEGMENTS = new Set(['login', 'signed-in']);

function extractPathname(callbackUrl?: string | null): string {
  if (!callbackUrl) {
    return '';
  }
  let raw = callbackUrl.trim();
  if (!raw) {
    return '';
  }
  try {
    if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(raw)) {
      raw = new URL(raw).pathname;
    }
  } catch {
    return '';
  }
  const path = raw.split('?')[0]?.split('#')[0] ?? '';
  if (!path || path === '/') {
    return '/';
  }
  return path.replace(/\/+$/, '') || '/';
}

function pathSegments(pathname: string): string[] {
  return pathname.split('/').filter(Boolean);
}

function isLocaleRoot(segments: string[]): boolean {
  return segments.length === 1 && LOCALES.includes(segments[0] as (typeof LOCALES)[number]);
}

function isGenericSegments(segments: string[]): boolean {
  if (segments.length === 0) {
    return true;
  }
  if (isLocaleRoot(segments)) {
    return true;
  }
  if (segments.length === 1) {
    return GENERIC_SEGMENTS.has(segments[0]);
  }
  if (segments.length === 2 && LOCALES.includes(segments[0] as (typeof LOCALES)[number])) {
    return GENERIC_SEGMENTS.has(segments[1]);
  }
  return false;
}

export function isGenericPostLoginPath(callbackUrl?: string | null): boolean {
  const pathname = extractPathname(callbackUrl);
  if (!pathname) {
    return true;
  }
  return isGenericSegments(pathSegments(pathname));
}

export function toAppHref(callbackUrl?: string | null): string {
  const pathname = extractPathname(callbackUrl);
  if (!pathname || pathname === '/') {
    return '/';
  }
  const segments = pathSegments(pathname);
  if (segments.length && LOCALES.includes(segments[0] as (typeof LOCALES)[number])) {
    const rest = segments.slice(1);
    return rest.length ? `/${rest.join('/')}` : '/';
  }
  return pathname.startsWith('/') ? pathname : `/${pathname}`;
}

export function localeFromCallbackUrl(callbackUrl: string | undefined | null, fallback: string): string {
  const pathname = extractPathname(callbackUrl);
  const first = pathSegments(pathname)[0];
  if (first && LOCALES.includes(first as (typeof LOCALES)[number])) {
    return first;
  }
  return fallback;
}

export function postLoginHref(isAdmin: boolean, callbackUrl?: string | null): string {
  if (isAdmin && isGenericPostLoginPath(callbackUrl)) {
    return '/admin';
  }
  if (callbackUrl && !isGenericPostLoginPath(callbackUrl)) {
    return toAppHref(callbackUrl);
  }
  return '/';
}

export function resolveAuthJsRedirect(url: string, baseUrl: string): string {
  let target: URL;
  try {
    target = url.startsWith('/') ? new URL(url, baseUrl) : new URL(url);
  } catch {
    return baseUrl;
  }
  const base = new URL(baseUrl);
  if (target.origin !== base.origin) {
    return baseUrl;
  }
  const path = target.pathname.replace(/\/+$/, '') || '/';
  const segments = pathSegments(path);
  const isSignedInPage =
    segments[0] === 'signed-in' ||
    (segments.length >= 2 && LOCALES.includes(segments[0] as (typeof LOCALES)[number]) && segments[1] === 'signed-in');
  if (isSignedInPage) {
    return `${target.pathname}${target.search}${target.hash}`;
  }
  if (isGenericPostLoginPath(path)) {
    const locale = path === '/en' || path.startsWith('/en/') ? 'en' : 'ar';
    const signedIn = locale === 'en' ? '/en/signed-in' : '/signed-in';
    const explicitNext = target.searchParams.get('callbackUrl') ?? target.searchParams.get('next');
    const next = explicitNext || `${path === '/' ? '/' : path}${target.search}`;
    return `${signedIn}?next=${encodeURIComponent(next)}`;
  }
  return `${target.pathname}${target.search}${target.hash}`;
}
