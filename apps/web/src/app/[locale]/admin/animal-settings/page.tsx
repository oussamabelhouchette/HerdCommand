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

  const [breedsResult, activeResult, statusesResult, farmsResult] = await Promise.allSettled([
    listBreeds(token, locale, { sort: 'displayOrder,asc' }),
    listBreeds(token, locale, { active: true, size: 1 }),
    listStatuses(token, locale, { sort: 'displayOrder,asc' }),
    listFarms(token, locale),
  ]);

  if (breedsResult.status === 'fulfilled') {
    initialBreedPage = breedsResult.value;
  } else {
    initialBreedError =
      breedsResult.reason instanceof ApiRequestError ? breedsResult.reason.message : t('loadError');
  }
  if (activeResult.status === 'fulfilled') {
    initialActiveBreedCount = activeResult.value.total;
  }
  if (statusesResult.status === 'fulfilled') {
    initialStatusPage = statusesResult.value;
  } else {
    initialStatusError =
      statusesResult.reason instanceof ApiRequestError ? statusesResult.reason.message : t('statusLoadError');
  }
  if (farmsResult.status === 'fulfilled') {
    farmId = farmsResult.value[0]?.id;
    if (farmId) {
      try {
        initialGroupPage = await listGroups(token, locale, farmId);
      } catch (error) {
        initialGroupError = error instanceof ApiRequestError ? error.message : t('groupLoadError');
      }
    } else {
      initialGroupError = t('farmLoadError');
    }
  } else {
    initialGroupError =
      farmsResult.reason instanceof ApiRequestError ? farmsResult.reason.message : t('groupLoadError');
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
