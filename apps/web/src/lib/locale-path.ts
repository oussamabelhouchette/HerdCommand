const LOCALES = ['ar', 'en'] as const;
const DEFAULT_LOCALE = 'ar';

/**
 * Server actions do not run next-intl middleware, so the default locale
 * (`/farm/...`) does not match `app/[locale]/...`. Always prefix.
 */
export function prefixedHref(href: string, locale: string) {
  const safeLocale = LOCALES.includes(locale as (typeof LOCALES)[number]) ? locale : DEFAULT_LOCALE;
  const queryIndex = href.indexOf('?');
  const path = queryIndex === -1 ? href : href.slice(0, queryIndex);
  const search = queryIndex === -1 ? '' : href.slice(queryIndex);
  const normalized = path.startsWith('/') ? path : `/${path}`;
  if (normalized === `/${safeLocale}` || normalized.startsWith(`/${safeLocale}/`)) {
    return `${normalized}${search}`;
  }
  return `/${safeLocale}${normalized}${search}`;
}
