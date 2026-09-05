import { getLocale, getTranslations } from 'next-intl/server';
import { headers } from 'next/headers';
import { getPathname } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
import styles from './Button.module.css';

function appPath(pathname: string): '/' | `/${string}` {
  const segments = pathname.split('/').filter(Boolean);
  if (segments[0] === 'en' || segments[0] === 'ar') {
    const rest = segments.slice(1);
    return rest.length ? `/${rest.join('/')}` : '/';
  }
  return (pathname.startsWith('/') ? pathname : `/${pathname}`) as `/${string}`;
}

export async function LanguageSwitcher() {
  const locale = await getLocale();
  const t = await getTranslations('nav');
  const pathname = (await headers()).get('x-pathname') ?? '/';
  const href = appPath(pathname);

  return (
    <div className="hc-row" role="group" aria-label={t('language')}>
      {routing.locales.map((code) => (
        <a
          key={code}
          href={getPathname({ href, locale: code })}
          className={`${styles.button} ${locale === code ? styles.primary : styles.tertiary}`}
          aria-current={locale === code ? 'page' : undefined}
        >
          {code === 'ar' ? 'العربية' : 'English'}
        </a>
      ))}
    </div>
  );
}
