import { cache } from 'react';
import { apiFetch } from './api';

export const ANIMAL_MANAGEMENT_FEATURE = 'ANIMAL_MANAGEMENT';

export type OwnerFarm = {
  id: string;
  code: string;
  name: string;
  nameAr: string;
  nameEn: string;
  nameFr?: string | null;
  governorateCode?: string | null;
  address?: string | null;
  timezone?: string | null;
  defaultLanguage?: string | null;
  currencyCode?: string | null;
  status: string;
  active: boolean;
  planCode?: string | null;
  maxActiveAnimals: number;
  enabledFeatureCodes: string[];
  animalManagementEnabled: boolean;
  createdAt: string;
  version: number;
};

export const listOwnerFarms = cache(async (accessToken: string, locale: string) => {
  return apiFetch<OwnerFarm[]>('/api/v1/me/farms', { accessToken, locale });
});

export const loadOwnerFarms = cache(async (accessToken: string, locale: string) => {
  try {
    return { farms: await listOwnerFarms(accessToken, locale) };
  } catch (error) {
    return { farms: [] as OwnerFarm[], error };
  }
});

export function farmIdFromPath(pathname: string): string | undefined {
  const match = pathname.match(/^\/farm\/([^/]+)/);
  return match?.[1];
}

export function farmHref(farmId: string, pathname = '/farm'): string {
  const suffix = pathname.replace(/^\/farm\/[^/]+/, '') || '';
  return `/farm/${farmId}${suffix}`;
}

export function findOwnerFarm(farms: OwnerFarm[], farmId?: string | null): OwnerFarm | undefined {
  if (!farmId) {
    return undefined;
  }
  return farms.find((farm) => farm.id === farmId);
}

export function firstOwnerFarm(farms: OwnerFarm[]): OwnerFarm | undefined {
  return farms[0];
}
