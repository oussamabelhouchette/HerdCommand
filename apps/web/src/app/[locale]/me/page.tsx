import { getTranslations, setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { logoutAction } from '@/lib/logout';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { isAuthSessionError } from '@/lib/refresh-access-token';

type Props = { params: Promise<{ locale: string }> };

async function loadMe(accessToken?: string) {
  if (!accessToken) {
    return null;
  }
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
  const response = await fetch(`${base}/api/v1/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: 'no-store',
  });
  if (response.status === 401) {
    return { unauthorized: true as const };
  }
  if (!response.ok) {
    return { error: true as const };
  }
  return response.json() as Promise<{
    subject: string;
    username: string;
    email?: string;
    roles: string[];
  }>;
}

export default async function MePage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await auth();
  const t = await getTranslations();

  if (!session?.user) {
    redirect({ href: '/login', locale });
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
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
