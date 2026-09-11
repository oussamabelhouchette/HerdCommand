import { setRequestLocale } from 'next-intl/server';
import { getSession } from '@/lib/session';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { membershipsFromSession } from '@/lib/me';
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
  const session = await getSession();
  const query = await searchParams;
  const intended = query.next ?? query.callbackUrl;

  if (!session?.user) {
    redirect({ href: '/login', locale });
    return null;
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
    return null;
  }

  const href = postLoginHref(await membershipsFromSession(session), intended);
  const destLocale = localeFromCallbackUrl(intended, locale);
  redirect({ href, locale: destLocale });
}
