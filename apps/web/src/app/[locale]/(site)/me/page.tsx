import { getTranslations, setRequestLocale } from 'next-intl/server';
import { getSession } from '@/lib/session';
import { logoutAction } from '@/lib/logout';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { loadMe } from '@/lib/me';

type Props = { params: Promise<{ locale: string }> };

export default async function MePage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await getSession();
  const t = await getTranslations();

  if (!session?.user) {
    redirect({ href: '/login', locale });
    return null;
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
    return null;
  }

  const me = await loadMe(session.accessToken);

  if (me && 'unauthorized' in me) {
    redirect({ href: '/login', locale });
  }

  return (
    <Card title={t('me.title')}>
      {session.error === 'SessionExpired' ? (
        <p className="hc-alert" data-tone="error">
          {t('auth.sessionExpired')}
        </p>
      ) : null}
      {me && 'error' in me ? <p>{t('me.apiError')}</p> : null}
      {me && 'subject' in me ? (
        <dl className="hc-stack">
          <div>
            <dt className="hc-muted">{t('me.subject')}</dt>
            <dd>{me.subject}</dd>
          </div>
          <div>
            <dt className="hc-muted">{t('me.username')}</dt>
            <dd>{me.username}</dd>
          </div>
          <div>
            <dt className="hc-muted">{t('me.email')}</dt>
            <dd>{me.email ?? '—'}</dd>
          </div>
          <div>
            <dt className="hc-muted">{t('me.roles')}</dt>
            <dd>{me.roles.join(', ') || '—'}</dd>
          </div>
        </dl>
      ) : (
        <p className="hc-muted">{t('me.apiError')}</p>
      )}
      <form action={logoutAction} style={{ marginTop: 24 }}>
        <Button type="submit" variant="secondary">
          {t('nav.signOut')}
        </Button>
      </form>
    </Card>
  );
}
