import { getTranslations, setRequestLocale } from 'next-intl/server';
import { auth } from '@/auth';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { redirect } from '@/i18n/navigation';
import { Card } from '@/components/Card';
import { SignInButton } from '@/components/SignInButton';

type Props = {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ callbackUrl?: string }>;
};

export default async function LoginPage({ params, searchParams }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const session = await auth();
  const { callbackUrl } = await searchParams;
  const target = callbackUrl || '/';

  if (session?.user && !isAuthSessionError(session.error)) {
    redirect({ href: target, locale });
  }

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
