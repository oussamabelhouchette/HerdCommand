import { setRequestLocale } from 'next-intl/server';
import { getSession } from '@/lib/session';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { membershipsFromSession } from '@/lib/me';
import { DEFAULT_PORTAL } from '@/lib/portals';
import { postLoginHref } from '@/lib/post-login-redirect';
import { FarmPortalCard } from '@/components/FarmPortalCard';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';

type Props = { params: Promise<{ locale: string }> };

export default async function HomePage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await getSession();

  if (!session?.user) {
    redirect({ href: '/login', locale });
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
  }

  const href = postLoginHref(await membershipsFromSession(session));
  if (href !== DEFAULT_PORTAL) {
    redirect({ href, locale });
  }

  return (
    <FarmPortalCard
      name={session.user.name || session.user.email || ''}
      idToken={session.idToken}
      locale={locale}
    />
  );
}
