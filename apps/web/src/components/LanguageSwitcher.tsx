'use client';

import { useLocale, useTranslations } from 'next-intl';
import { usePathname, useRouter } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
import { Button } from './Button';

export function LanguageSwitcher() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const t = useTranslations('nav');

  function switchTo(next: 'ar' | 'en') {
    router.replace(pathname, { locale: next });
  }

  return (
    <div className="hc-row" role="group" aria-label={t('language')}>
      {routing.locales.map((code) => (
        <Button
          key={code}
          variant={locale === code ? 'primary' : 'tertiary'}
          aria-pressed={locale === code}
          onClick={() => switchTo(code)}
        >
          {code === 'ar' ? 'العربية' : 'English'}
        </Button>
      ))}
    </div>
  );
}
