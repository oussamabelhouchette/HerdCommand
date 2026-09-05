import { getTranslations, setRequestLocale } from 'next-intl/server';
import { Link } from '@/i18n/navigation';
import { requireAdmin } from '@/lib/require-admin';
import { AdminShell } from '@/components/admin/AdminShell';
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
            <Link href="/">{t('backHome')}</Link>
          </p>
        </section>
      </div>
    );
  }

  return <AdminShell me={gate.me}>{children}</AdminShell>;
}
