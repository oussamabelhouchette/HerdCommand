import { getTranslations, setRequestLocale } from 'next-intl/server';
import { GuestHeader } from '@/components/GuestHeader';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function AuthLayout({ children, params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations();

  return (
    <div className="hc-shell">
      <GuestHeader
        locale={locale}
        name={t('app.name')}
        tagline={t('app.tagline')}
        homeLabel={t('nav.home')}
        accountLabel={t('nav.account')}
        languageLabel={t('nav.language')}
        signInLabel={t('nav.signIn')}
      />
      <main>{children}</main>
    </div>
  );
}
