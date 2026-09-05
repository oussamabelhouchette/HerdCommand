import { getTranslations, setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { logoutAction } from '@/lib/logout';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { isMeIdentity, loadMe } from '@/lib/me';
import { isAdminRole } from '@/lib/roles';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';

type Props = { params: Promise<{ locale: string }> };

export default async function HomePage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations();
  const session = await auth();

  if (!session?.user) {
    redirect({ href: '/login', locale });
    return null;
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
    return null;
  }

  if (isAdminRole(session.roles)) {
    redirect({ href: '/admin', locale });
  }

  const me = await loadMe(session.accessToken);
  if (isMeIdentity(me) && isAdminRole(me.roles)) {
    redirect({ href: '/admin', locale });
  }

  return (
    <Card title={t('auth.welcome')}>
      <p>{session.user.name || session.user.email}</p>
      <form action={logoutAction}>
        <Button type="submit" variant="secondary">
          {t('nav.signOut')}
        </Button>
      </form>
    </Card>
  );
}
