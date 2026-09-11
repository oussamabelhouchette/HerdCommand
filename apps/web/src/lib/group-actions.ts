'use server';

import { revalidatePath } from 'next/cache';
import { getSession } from '@/lib/session';
import { ApiRequestError } from '@/lib/api';
import {
  DEFAULT_GROUP_TYPE,
  createGroup,
  groupsHref,
  updateGroup,
  updateGroupActive,
} from '@/lib/groups';
import { redirect } from '@/i18n/navigation';

function blankToUndefined(value: FormDataEntryValue | null) {
  const text = typeof value === 'string' ? value.trim() : '';
  return text ? text : undefined;
}

function parsedCapacity(value: FormDataEntryValue | null) {
  const text = typeof value === 'string' ? value.trim() : '';
  if (!text) {
    return null;
  }
  const capacity = Number.parseInt(text, 10);
  return Number.isFinite(capacity) ? capacity : null;
}

export async function saveGroupAction(farmId: string, locale: string, groupId: string, formData: FormData) {
  const session = await getSession();
  if (!session?.accessToken) {
    redirect({ href: '/login', locale });
    return;
  }

  const body = {
    nameAr: String(formData.get('nameAr') ?? '').trim(),
    nameEn: String(formData.get('nameEn') ?? '').trim(),
    groupTypeCode: DEFAULT_GROUP_TYPE,
    description: blankToUndefined(formData.get('description')),
    capacity: parsedCapacity(formData.get('capacity')),
  };

  try {
    if (groupId) {
      await updateGroup(session.accessToken, locale, farmId, groupId, body);
    } else {
      await createGroup(session.accessToken, locale, farmId, {
        ...body,
        code: String(formData.get('code') ?? '').trim().toUpperCase(),
      });
    }
  } catch (error) {
    const message = error instanceof ApiRequestError ? error.message : 'save';
    redirect({
      href: groupsHref(farmId, {
        compose: groupId ? undefined : 'new',
        edit: groupId || undefined,
        error: message,
      }),
      locale,
    });
    return;
  }

  revalidatePath(`/${locale}/farm/${farmId}/groups`);
  revalidatePath(`/${locale}/farm/${farmId}/animals`);
  revalidatePath(`/${locale}/farm/${farmId}`);
  redirect({ href: groupsHref(farmId), locale });
}

export async function setGroupActiveAction(farmId: string, locale: string, groupId: string, active: boolean) {
  const session = await getSession();
  if (!session?.accessToken) {
    redirect({ href: '/login', locale });
    return;
  }
  try {
    await updateGroupActive(session.accessToken, locale, farmId, groupId, active);
  } catch (error) {
    const message = error instanceof ApiRequestError ? error.message : 'status';
    redirect({ href: groupsHref(farmId, { error: message }), locale });
    return;
  }
  revalidatePath(`/${locale}/farm/${farmId}/groups`);
  revalidatePath(`/${locale}/farm/${farmId}/animals`);
  revalidatePath(`/${locale}/farm/${farmId}`);
  redirect({ href: groupsHref(farmId), locale });
}
