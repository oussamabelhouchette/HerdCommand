import { getTranslations, setRequestLocale } from 'next-intl/server';
import { ApiRequestError } from '@/lib/api';
import { emptyBreedPage, listBreeds } from '@/lib/breeds';
import { requireAdmin } from '@/lib/require-admin';
import { BreedManagement } from '@/components/admin/BreedManagement';

type Props = { params: Promise<{ locale: string }> };

export default async function AnimalSettingsPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireAdmin(locale);
  const t = await getTranslations('animalSettings');

  if (!gate.allowed || !gate.session.accessToken) {
    return null;
  }

  let initialPage = emptyBreedPage();
  let initialActiveCount = 0;
  let initialError: string | undefined;

  try {
    const token = gate.session.accessToken;
    initialPage = await listBreeds(token, locale, { sort: 'displayOrder,asc' });
    initialActiveCount = initialPage.items.filter((breed) => breed.active).length;
  } catch (error) {
    initialError = error instanceof ApiRequestError ? error.message : t('loadError');
  }

  return (
    <BreedManagement
      initialPage={initialPage}
      initialActiveCount={initialActiveCount}
      initialError={initialError}
    />
  );
}
