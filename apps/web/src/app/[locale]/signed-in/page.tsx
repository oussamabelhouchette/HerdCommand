import { setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { isAdminRole } from '@/lib/roles';
import { isMeIdentity, loadMe } from '@/lib/me';
import { localeFromCallbackUrl, postLoginHref } from '@/lib/post-login-redirect';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';

type Props = {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ next?: string; callbackUrl?: string }>;
};

export default async function SignedInPage({ params, searchParams }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await auth();
  const query = await searchParams;
  const intended = query.next ?? query.callbackUrl;

  if (!session?.user) {
    redirect({ href: '/login', locale });
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
  }

  let admin = isAdminRole(session.roles);
  if (!admin && session.accessToken) {
    const me = await loadMe(session.accessToken);
    admin = isMeIdentity(me) && isAdminRole(me.roles);
  }

  const href = postLoginHref(admin, intended);
  const destLocale = localeFromCallbackUrl(intended, locale);
  redirect({ href, locale: destLocale });
}
