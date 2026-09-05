import { getLocale, getTranslations } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { logoutAction } from '@/lib/logout';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { membershipsFromSession } from '@/lib/me';
import { isAdminPortal, portalHref } from '@/lib/portals';
import { getSession } from '@/lib/session';
import { Button } from './Button';
import { LanguageSwitcher } from './LanguageSwitcher';

export async function AppHeader() {
  const t = await getTranslations();
  const locale = await getLocale();
  const session = await getSession();
  const memberships = await membershipsFromSession(session);
  const homeHref = session?.user ? portalHref(memberships) : '/login';
  const showAdmin = isAdminPortal(homeHref);

  function href(path: '/' | `/${string}`) {
    return getPathname({ href: path, locale });
  }

  return (
    <header className="hc-header">
      <a href={href(homeHref as '/' | `/${string}`)} className="hc-brand">
        <strong>{t('app.name')}</strong>
        <span className="hc-muted">{t('app.tagline')}</span>
      </a>
      <nav className="hc-nav" aria-label={t('app.name')}>
        <a href={href(homeHref as '/' | `/${string}`)}>{t('nav.home')}</a>
        {showAdmin ? <a href={href('/admin/animal-settings')}>{t('nav.animalSettings')}</a> : null}
        <a href={href('/me')}>{t('nav.account')}</a>
        <LanguageSwitcher />
        {session?.user && !isAuthSessionError(session.error) ? (
          <form action={logoutAction}>
            <Button type="submit" variant="secondary">
              {t('nav.signOut')}
            </Button>
          </form>
        ) : (
          <a href={href('/login')}>{t('nav.signIn')}</a>
        )}
      </nav>
    </header>
  );
}
