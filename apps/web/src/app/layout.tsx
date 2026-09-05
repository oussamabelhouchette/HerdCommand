import '@herdcommand/tokens/css';
import './globals.css';
import { IBM_Plex_Sans_Arabic } from 'next/font/google';
import { getLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

const arabic = IBM_Plex_Sans_Arabic({
  subsets: ['arabic'],
  weight: ['400', '600'],
  variable: '--font-arabic',
  display: 'optional',
  preload: true,
  adjustFontFallback: true,
});

export const viewport = {
  width: 'device-width',
  initialScale: 1,
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();
  const dir = locale === 'ar' ? 'rtl' : 'ltr';

  return (
    <html lang={locale} dir={dir} className={locale === 'ar' ? arabic.variable : undefined}>
      <body suppressHydrationWarning>{children}</body>
    </html>
  );
}
