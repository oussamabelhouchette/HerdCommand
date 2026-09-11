import { setRequestLocale } from 'next-intl/server';
import { requireCurrentFarm } from '@/lib/farm-portal';
import { parseGroupSearchParams } from '@/lib/groups';
import { OwnerGroups } from '@/components/farm/OwnerGroups';

type Props = {
  params: Promise<{ locale: string; farmId: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function FarmGroupsPage({ params, searchParams }: Props) {
  const { locale, farmId } = await params;
  setRequestLocale(locale);
  const current = await requireCurrentFarm(locale, farmId);
  if (!current.ok) {
    return null;
  }

  return <OwnerGroups farm={current.farm} query={parseGroupSearchParams(await searchParams)} />;
}
