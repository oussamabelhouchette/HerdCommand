import { setRequestLocale } from 'next-intl/server';
import { loadOwnerFarms, selectOwnerFarm } from '@/lib/owner-farms';
import { requireFarmOwner } from '@/lib/require-admin';
import { FarmAnimals } from '@/components/farm/FarmAnimals';
import { redirect } from '@/i18n/navigation';

type Props = { params: Promise<{ locale: string; farmId: string }> };

export default async function FarmAnimalsPage({ params }: Props) {
  const { locale, farmId } = await params;
  setRequestLocale(locale);
  const gate = await requireFarmOwner(locale);

  if (!gate.allowed || !gate.session.accessToken) {
    return null;
  }

  const { farms } = await loadOwnerFarms(gate.session.accessToken, locale);
  const farm = selectOwnerFarm(farms, farmId);
  if (!farm) {
    redirect({ href: '/farm', locale });
  }
  if (farm.id !== farmId) {
    redirect({ href: `/farm/${farm.id}/animals`, locale });
  }

  return <FarmAnimals farm={farm} />;
}
