import { setRequestLocale } from 'next-intl/server';
import { requireCurrentFarm } from '@/lib/farm-portal';
import { FarmDashboard } from '@/components/farm/FarmDashboard';

type Props = { params: Promise<{ locale: string; farmId: string }> };

export default async function FarmDashboardPage({ params }: Props) {
  const { locale, farmId } = await params;
  setRequestLocale(locale);
  const current = await requireCurrentFarm(locale, farmId);
  if (!current.ok) {
    return null;
  }
  return <FarmDashboard farm={current.farm} />;
}
