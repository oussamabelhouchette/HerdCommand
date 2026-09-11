'use server';

import { revalidatePath } from 'next/cache';
import { getSession } from '@/lib/session';
import { ApiRequestError } from '@/lib/api';
import { archiveFarmAnimal, animalsHref, createFarmAnimal, updateFarmAnimal } from '@/lib/animals';
import { prefixedHref } from '@/lib/locale-path';
import { redirect } from 'next/navigation';

function blankToNull(value: FormDataEntryValue | null) {
  const text = typeof value === 'string' ? value.trim() : '';
  return text ? text : null;
}

export async function saveAnimalAction(farmId: string, locale: string, animalId: string, formData: FormData) {
  const session = await getSession();
  if (!session?.accessToken) {
    redirect(prefixedHref('/login', locale));
    return;
  }

  const body = {
    identificationNumber: String(formData.get('identificationNumber') ?? '').trim(),
    name: blankToNull(formData.get('name')),
    breedId: String(formData.get('breedId') ?? ''),
    statusCode: String(formData.get('statusCode') ?? ''),
    genderCode: String(formData.get('genderCode') ?? ''),
    dateOfBirth: blankToNull(formData.get('dateOfBirth')),
    groupId: blankToNull(formData.get('groupId')),
  };

  try {
    if (animalId) {
      await updateFarmAnimal(session.accessToken, locale, farmId, animalId, body);
    } else {
      await createFarmAnimal(session.accessToken, locale, farmId, body);
    }
  } catch (error) {
    const message = error instanceof ApiRequestError ? error.message : 'save';
    redirect(
      prefixedHref(
        animalsHref(farmId, {
          compose: animalId ? undefined : 'new',
          edit: animalId || undefined,
          error: message,
        }),
        locale,
      ),
    );
    return;
  }

  revalidatePath(`/${locale}/farm/${farmId}/animals`);
  redirect(prefixedHref(animalsHref(farmId), locale));
}

export async function archiveAnimalAction(farmId: string, locale: string, animalId: string) {
  const session = await getSession();
  if (!session?.accessToken) {
    redirect(prefixedHref('/login', locale));
    return;
  }
  try {
    await archiveFarmAnimal(session.accessToken, locale, farmId, animalId);
  } catch (error) {
    const message = error instanceof ApiRequestError ? error.message : 'archive';
    redirect(prefixedHref(animalsHref(farmId, { error: message }), locale));
    return;
  }
  revalidatePath(`/${locale}/farm/${farmId}/animals`);
  redirect(prefixedHref(animalsHref(farmId), locale));
}
