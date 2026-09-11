import { setRequestLocale } from 'next-intl/server';
import { requireCurrentFarm } from '@/lib/farm-portal';
import { parseAnimalSearchParams } from '@/lib/animals';
import { OwnerAnimals } from '@/components/farm/OwnerAnimals';

type Props = {
  params: Promise<{ locale: string; farmId: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function FarmAnimalsPage({ params, searchParams }: Props) {
  const { locale, farmId } = await params;
  setRequestLocale(locale);
  const current = await requireCurrentFarm(locale, farmId);
  if (!current.ok) {
    return null;
  }

  return <OwnerAnimals farm={current.farm} query={parseAnimalSearchParams(await searchParams)} />;
}
