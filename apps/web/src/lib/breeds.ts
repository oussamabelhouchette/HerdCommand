import { apiFetch } from './api';

export const SPECIES_CODES = ['SHEEP', 'GOAT', 'CATTLE', 'CAMEL'] as const;
export type SpeciesCode = (typeof SPECIES_CODES)[number];

export type Breed = {
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  speciesCode: SpeciesCode;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
};

export type BreedPage = {
  items: Breed[];
  total: number;
  page: number;
  size: number;
  sort: string;
};

export type BreedListQuery = {
  search?: string;
  speciesCode?: SpeciesCode | '';
  active?: boolean | '';
  page?: number;
  size?: number;
  sort?: string;
};

export type CreateBreedInput = {
  code: string;
  nameAr: string;
  nameEn: string;
  speciesCode: SpeciesCode;
  displayOrder: number;
};

export type UpdateBreedInput = {
  nameAr: string;
  nameEn: string;
  speciesCode: SpeciesCode;
  displayOrder: number;
};

const BASE = '/api/v1/admin/animal-breeds';

export function emptyBreedPage(): BreedPage {
  return { items: [], total: 0, page: 0, size: 20, sort: 'displayOrder,asc' };
}

export function listBreeds(accessToken: string, locale: string, query: BreedListQuery = {}) {
  return apiFetch<BreedPage>(BASE, {
    accessToken,
    locale,
    query: {
      search: query.search,
      speciesCode: query.speciesCode || undefined,
      active: query.active === '' || query.active === undefined ? undefined : query.active,
      page: query.page ?? 0,
      size: query.size ?? 20,
      sort: query.sort ?? 'displayOrder,asc',
    },
  });
}

export function createBreed(accessToken: string, locale: string, body: CreateBreedInput) {
  return apiFetch<Breed>(BASE, { accessToken, locale, method: 'POST', body });
}

export function updateBreed(accessToken: string, locale: string, id: string, body: UpdateBreedInput) {
  return apiFetch<Breed>(`${BASE}/${id}`, { accessToken, locale, method: 'PUT', body });
}

export function updateBreedStatus(accessToken: string, locale: string, id: string, active: boolean) {
  return apiFetch<Breed>(`${BASE}/${id}/status`, {
    accessToken,
    locale,
    method: 'PATCH',
    body: { active },
  });
}
