import { getTranslations, setRequestLocale } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { Card } from '@/components/Card';
import { RedirectIfSignedIn } from '@/components/RedirectIfSignedIn';
import { SignInButton } from '@/components/SignInButton';

type Props = {
  params: Promise<{ locale: string }>;
};

export const dynamic = 'force-static';
export const revalidate = 300;

export default async function LoginPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const continuePath = getPathname({ href: '/signed-in', locale });
  const t = await getTranslations();

  return (
    <div className="hc-login">
      <RedirectIfSignedIn />
      <Card title={t('auth.title')}>
        <p>{t('auth.subtitle')}</p>
        <p className="hc-muted">{t('auth.hint')}</p>
        <p className="hc-muted">{t('auth.demo')}</p>
        <SignInButton label={t('nav.signIn')} continuePath={continuePath} />
      </Card>
    </div>
  );
}
