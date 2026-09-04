import { defineRouting } from 'next-intl/routing';

export const routing = defineRouting({
  locales: ['ar', 'en'],
  defaultLocale: 'ar',
  localePrefix: 'as-needed',
  localeDetection: false,
  localeCookie: {
    name: 'HERDCOMMAND_LOCALE',
    maxAge: 60 * 60 * 24 * 365,
  },
});
