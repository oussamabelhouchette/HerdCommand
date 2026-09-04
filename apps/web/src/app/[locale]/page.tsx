import { getTranslations, setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { logoutAction } from '@/lib/logout';
import { isAuthSessionError } from '@/lib/refresh-access-token';
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
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
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
