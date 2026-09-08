import { setRequestLocale } from 'next-intl/server';
import { requireAdminShell } from '@/lib/require-admin';
import { isAdminRole, isPlatformAdminRole } from '@/lib/roles';
import { redirect } from '@/i18n/navigation';

type Props = { params: Promise<{ locale: string }> };

export default async function AdminIndexPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireAdminShell(locale);
  const roles = gate.allowed ? gate.me.roles : [];
  const href =
    isPlatformAdminRole(roles) && !isAdminRole(roles) ? '/admin/farm-settings' : '/admin/animal-settings';
  redirect({ href, locale });
}
