import { getPathname } from '@/i18n/navigation';
import { routing } from '@/i18n/routing';
import styles from './Button.module.css';

type Props = {
  locale: string;
  name: string;
  tagline: string;
  homeLabel: string;
  accountLabel: string;
  languageLabel: string;
  signInLabel: string;
};

export function GuestHeader({
  locale,
  name,
  tagline,
  homeLabel,
  accountLabel,
  languageLabel,
  signInLabel,
}: Props) {
  const home = getPathname({ href: '/login', locale });
  const account = getPathname({ href: '/me', locale });
  const signIn = getPathname({ href: '/login', locale });

  return (
    <header className="hc-header">
      <a href={home} className="hc-brand">
        <strong>{name}</strong>
        <span className="hc-muted">{tagline}</span>
      </a>
      <nav className="hc-nav" aria-label={name}>
        <a href={home}>{homeLabel}</a>
        <a href={account}>{accountLabel}</a>
        <div className="hc-row" role="group" aria-label={languageLabel}>
          {routing.locales.map((code) => (
            <a
              key={code}
              href={getPathname({ href: '/login', locale: code })}
              className={`${styles.button} ${locale === code ? styles.primary : styles.tertiary}`}
              aria-current={locale === code ? 'page' : undefined}
            >
              {code === 'ar' ? 'العربية' : 'English'}
            </a>
          ))}
        </div>
        <a href={signIn}>{signInLabel}</a>
      </nav>
    </header>
  );
}
