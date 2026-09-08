import { getMessages, getTranslations, setRequestLocale } from 'next-intl/server';
import { NextIntlClientProvider } from 'next-intl';
import { getPathname } from '@/i18n/navigation';
import { loadOwnerFarms } from '@/lib/owner-farms';
import { requireFarmOwner } from '@/lib/require-admin';
import { AuthSessionProvider } from '@/components/AuthSessionProvider';
import { FarmShell } from '@/components/farm/FarmShell';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function FarmLayout({ children, params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireFarmOwner(locale);

  if (!gate.allowed) {
    const t = await getTranslations('farm');
    return (
      <div className="hc-shell">
        <section className="hc-login">
          <h1>{t('forbidden')}</h1>
          <p className="hc-muted">{t('forbiddenHint')}</p>
          <p>
            <a href={getPathname({ href: '/portal', locale })}>{t('backHome')}</a>
          </p>
        </section>
      </div>
    );
  }

  const messages = await getMessages();
  const farms = gate.session.accessToken
    ? (await loadOwnerFarms(gate.session.accessToken, locale)).farms
    : [];

  return (
    <NextIntlClientProvider
      locale={locale}
      messages={{
        farm: messages.farm,
        farmSettings: messages.farmSettings,
        nav: messages.nav,
        app: messages.app,
      }}
    >
      <AuthSessionProvider session={gate.session}>
        <FarmShell me={gate.me} farms={farms} idToken={gate.session.idToken} locale={locale}>
          {children}
        </FarmShell>
      </AuthSessionProvider>
    </NextIntlClientProvider>
  );
}
