import { getTranslations, setRequestLocale } from 'next-intl/server';
import { ApiRequestError } from '@/lib/api';
import { emptyBreedPage, listBreeds } from '@/lib/breeds';
import { emptyGroupPage, listFarms, listGroups } from '@/lib/groups';
import { emptyStatusPage, listStatuses } from '@/lib/statuses';
import { requireAdmin } from '@/lib/require-admin';
import { AnimalSettings } from '@/components/admin/AnimalSettings';

type Props = { params: Promise<{ locale: string }> };

export default async function AnimalSettingsPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireAdmin(locale);
  const t = await getTranslations('animalSettings');

  if (!gate.allowed || !gate.session.accessToken) {
    return null;
  }

  const token = gate.session.accessToken;
  let initialBreedPage = emptyBreedPage();
  let initialActiveBreedCount = 0;
  let initialBreedError: string | undefined;
  let initialStatusPage = emptyStatusPage();
  let initialStatusError: string | undefined;
  let farmId: string | undefined;
  let initialGroupPage = emptyGroupPage();
  let initialGroupError: string | undefined;

  try {
    const [page, active] = await Promise.all([
      listBreeds(token, locale, { sort: 'displayOrder,asc' }),
      listBreeds(token, locale, { active: true, size: 1 }),
    ]);
    initialBreedPage = page;
    initialActiveBreedCount = active.total;
  } catch (error) {
    initialBreedError = error instanceof ApiRequestError ? error.message : t('loadError');
  }

  try {
    initialStatusPage = await listStatuses(token, locale, { sort: 'displayOrder,asc' });
  } catch (error) {
    initialStatusError = error instanceof ApiRequestError ? error.message : t('statusLoadError');
  }

  try {
    const farms = await listFarms(token, locale);
    farmId = farms[0]?.id;
    if (farmId) {
      initialGroupPage = await listGroups(token, locale, farmId);
    } else {
      initialGroupError = t('farmLoadError');
    }
  } catch (error) {
    initialGroupError = error instanceof ApiRequestError ? error.message : t('groupLoadError');
  }

  return (
    <AnimalSettings
      farmId={farmId}
      initialBreedPage={initialBreedPage}
      initialActiveBreedCount={initialActiveBreedCount}
      initialBreedError={initialBreedError}
      initialStatusPage={initialStatusPage}
      initialStatusError={initialStatusError}
      initialGroupPage={initialGroupPage}
      initialGroupError={initialGroupError}
    />
  );
}
