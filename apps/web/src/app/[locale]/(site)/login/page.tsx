import { getTranslations, setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { getPathname, redirect } from '@/i18n/navigation';
import { Card } from '@/components/Card';
import { SignInButton } from '@/components/SignInButton';
import { isAdminRole } from '@/lib/roles';
import { isMeIdentity, loadMe } from '@/lib/me';
import { postLoginHref } from '@/lib/post-login-redirect';

type Props = {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ callbackUrl?: string }>;
};

export default async function LoginPage({ params, searchParams }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await auth();
  const { callbackUrl } = await searchParams;
  const intended = callbackUrl || getPathname({ href: '/', locale });

  if (session?.user && !isAuthSessionError(session.error)) {
    let admin = isAdminRole(session.roles);
    if (!admin && session.accessToken) {
      const me = await loadMe(session.accessToken);
      admin = isMeIdentity(me) && isAdminRole(me.roles);
    }
    redirect({ href: postLoginHref(admin, callbackUrl), locale });
  }

  const continuePath = getPathname({ href: '/signed-in', locale });
  const target = `${continuePath}?next=${encodeURIComponent(intended)}`;

  const t = await getTranslations();

  return (
    <div className="hc-login">
      <Card title={t('auth.title')}>
        <p>{t('auth.subtitle')}</p>
        <p className="hc-muted">{t('auth.hint')}</p>
        <p className="hc-muted">{t('auth.demo')}</p>
        {isAuthSessionError(session?.error) ? (
          <p className="hc-alert" data-tone="error">
            {t('auth.sessionExpired')}
          </p>
        ) : null}
        <SignInButton label={t('nav.signIn')} callbackUrl={target} />
      </Card>
    </div>
  );
}
