import { getMessages, getTranslations, setRequestLocale } from 'next-intl/server';
import { NextIntlClientProvider } from 'next-intl';
import { getPathname } from '@/i18n/navigation';
import { requireAdmin } from '@/lib/require-admin';
import { AdminShell } from '@/components/admin/AdminShell';
import { AuthSessionProvider } from '@/components/AuthSessionProvider';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function AdminLayout({ children, params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireAdmin(locale);

  if (!gate.allowed) {
    const t = await getTranslations('admin');
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
        animalSettings: messages.animalSettings,
        admin: messages.admin,
        nav: messages.nav,
        app: messages.app,
      }}
    >
      <AuthSessionProvider>
        <AdminShell me={gate.me}>{children}</AdminShell>
      </AuthSessionProvider>
    </NextIntlClientProvider>
  );
}
