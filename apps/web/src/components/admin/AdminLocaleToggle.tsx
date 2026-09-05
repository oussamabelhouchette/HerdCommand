'use client';

import { useLocale } from 'next-intl';
import { usePathname, useRouter } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
import styles from './AdminShell.module.css';

export function AdminLocaleToggle() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  return (
    <div className={styles.sideTools} role="group">
      {routing.locales.map((code) => (
        <button
          key={code}
          type="button"
          className={styles.localeBtn}
          aria-pressed={locale === code}
          onClick={() => router.replace(pathname, { locale: code })}
        >
          {code === 'ar' ? 'العربية' : 'English'}
        </button>
      ))}
    </div>
  );
}
