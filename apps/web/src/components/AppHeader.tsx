import { getTranslations } from 'next-intl/server';
import { auth } from '@/auth';
import { logoutAction } from '@/lib/logout';
import { Link } from '@/i18n/navigation';
import { Button } from './Button';
import { LanguageSwitcher } from './LanguageSwitcher';
import { isAuthSessionError } from '@/lib/refresh-access-token';

export async function AppHeader() {
  const t = await getTranslations();
  const session = await auth();

  return (
    <header className="hc-header">
      <Link href="/" className="hc-brand">
        <strong>{t('app.name')}</strong>
        <span className="hc-muted">{t('app.tagline')}</span>
      </Link>
      <nav className="hc-nav" aria-label={t('app.name')}>
        <Link href="/">{t('nav.home')}</Link>
        <Link href="/design-system">{t('nav.designSystem')}</Link>
        <Link href="/keycloak-login-preview">{t('nav.loginPreview')}</Link>
        <Link href="/me">{t('nav.account')}</Link>
        <LanguageSwitcher />
        {session?.user && !isAuthSessionError(session.error) ? (
          <form action={logoutAction}>
            <Button type="submit" variant="secondary">
              {t('nav.signOut')}
            </Button>
          </form>
        ) : (
          <Link href="/login">{t('nav.signIn')}</Link>
        )}
      </nav>
    </header>
  );
}
