import { getMessages, getTranslations, setRequestLocale } from 'next-intl/server';
import { NextIntlClientProvider } from 'next-intl';
import { getPathname } from '@/i18n/navigation';
import { loadFarmPortal } from '@/lib/farm-portal';
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
  const portal = await loadFarmPortal(locale);

  if (!portal.gate.allowed) {
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

  return (
    <NextIntlClientProvider
      locale={locale}
      messages={{
        farm: messages.farm,
        farmSettings: messages.farmSettings,
        animals: messages.animals,
        nav: messages.nav,
        app: messages.app,
      }}
    >
      <AuthSessionProvider session={portal.gate.session}>
        <FarmShell me={portal.gate.me} farms={portal.farms} idToken={portal.gate.session.idToken} locale={locale}>
          {children}
        </FarmShell>
      </AuthSessionProvider>
    </NextIntlClientProvider>
  );
}
