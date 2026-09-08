import { apiFetch } from './api';

export const ANIMAL_PAGE_SIZE = 20;
export const ANIMAL_SEARCH_DEBOUNCE_MS = 400;

export type AnimalBreedRef = {
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
};

export type AnimalGroupRef = {
  id: string;
  nameAr: string;
  nameEn: string;
};

export type AnimalStatusRef = {
  code: string;
  nameAr: string;
  nameEn: string;
  colorToken?: string;
};

export type FarmAnimal = {
  id: string;
  farmId: string;
  identificationNumber: string;
  name?: string | null;
  breed: AnimalBreedRef | null;
  genderCode: string;
  dateOfBirth?: string | null;
  ageDisplay: string;
  group: AnimalGroupRef | null;
  status: AnimalStatusRef | null;
  createdAt: string;
  updatedAt: string;
};

export type AnimalPage = {
  items: FarmAnimal[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sort: string;
};

export type AnimalLookups = {
  breeds: Array<{ id: string; code: string; nameAr: string; nameEn: string; active: boolean }>;
  statuses: Array<{ code: string; nameAr: string; nameEn: string; colorToken: string; active: boolean }>;
  groups: Array<{ id: string; nameAr: string; nameEn: string; active: boolean }>;
  genders: Array<{ code: string; nameAr: string; nameEn: string }>;
};

export type AnimalListQuery = {
  search?: string;
  breedId?: string;
  statusCode?: string;
  groupId?: string;
  gender?: string;
  page?: number;
};

export type AnimalSearchState = AnimalListQuery & {
  compose?: 'new' | 'edit';
  editId?: string;
  archiveId?: string;
  formError?: string;
};

function firstParam(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }
  return value ?? '';
}

export function parseAnimalSearchParams(
  search: Record<string, string | string[] | undefined>,
): AnimalSearchState {
  const pageValue = Number.parseInt(firstParam(search.page), 10);
  const editId = firstParam(search.edit) || undefined;
  return {
    search: firstParam(search.q).trim(),
    breedId: firstParam(search.breedId),
    statusCode: firstParam(search.statusCode),
    groupId: firstParam(search.groupId),
    gender: firstParam(search.gender),
    page: Number.isFinite(pageValue) && pageValue > 0 ? pageValue : 0,
    compose: editId ? 'edit' : firstParam(search.compose) === 'new' ? 'new' : undefined,
    editId,
    archiveId: firstParam(search.archive) || undefined,
    formError: firstParam(search.error) || undefined,
  };
}

export function animalsHref(farmId: string, query: AnimalListQuery & { compose?: string; edit?: string; archive?: string; error?: string } = {}) {
  const params = new URLSearchParams();
  if (query.search?.trim()) {
    params.set('q', query.search.trim());
  }
  if (query.breedId) {
    params.set('breedId', query.breedId);
  }
  if (query.statusCode) {
    params.set('statusCode', query.statusCode);
  }
  if (query.groupId) {
    params.set('groupId', query.groupId);
  }
  if (query.gender) {
    params.set('gender', query.gender);
  }
  if (query.page && query.page > 0) {
    params.set('page', String(query.page));
  }
  if (query.compose) {
    params.set('compose', query.compose);
  }
  if (query.edit) {
    params.set('edit', query.edit);
  }
  if (query.archive) {
    params.set('archive', query.archive);
  }
  if (query.error) {
    params.set('error', query.error);
  }
  const qs = params.toString();
  return qs ? `/farm/${farmId}/animals?${qs}` : `/farm/${farmId}/animals`;
}

export type AnimalInput = {
  identificationNumber: string;
  name?: string | null;
  breedId: string;
  statusCode: string;
  genderCode: string;
  dateOfBirth?: string | null;
  groupId?: string | null;
};

export function emptyAnimalPage(): AnimalPage {
  return {
    items: [],
    total: 0,
    page: 0,
    size: ANIMAL_PAGE_SIZE,
    totalPages: 0,
    first: true,
    last: true,
    sort: 'createdAt,desc',
  };
}

export function emptyAnimalLookups(): AnimalLookups {
  return { breeds: [], statuses: [], groups: [], genders: [] };
}

export function normalizeLookups(value?: Partial<AnimalLookups> | null): AnimalLookups {
  const empty = emptyAnimalLookups();
  return {
    breeds: Array.isArray(value?.breeds) ? value.breeds : empty.breeds,
    statuses: Array.isArray(value?.statuses) ? value.statuses : empty.statuses,
    groups: Array.isArray(value?.groups) ? value.groups : empty.groups,
    genders: Array.isArray(value?.genders) ? value.genders : empty.genders,
  };
}

export function normalizePage(value?: Partial<AnimalPage> | null): AnimalPage {
  const empty = emptyAnimalPage();
  return {
    items: Array.isArray(value?.items) ? value.items : empty.items,
    total: typeof value?.total === 'number' ? value.total : empty.total,
    page: typeof value?.page === 'number' ? value.page : empty.page,
    size: typeof value?.size === 'number' ? value.size : empty.size,
    totalPages: typeof value?.totalPages === 'number' ? value.totalPages : empty.totalPages,
    first: value?.first ?? empty.first,
    last: value?.last ?? empty.last,
    sort: value?.sort ?? empty.sort,
  };
}

function animalsBase(farmId: string) {
  return `/api/v1/farms/${farmId}/animals`;
}

export function listFarmAnimals(accessToken: string, locale: string, farmId: string, query: AnimalListQuery = {}) {
  return apiFetch<AnimalPage>(animalsBase(farmId), {
    accessToken,
    locale,
    query: {
      search: query.search?.trim() || undefined,
      breedId: query.breedId || undefined,
      statusCode: query.statusCode || undefined,
      groupId: query.groupId || undefined,
      gender: query.gender || undefined,
      page: query.page ?? 0,
      size: ANIMAL_PAGE_SIZE,
    },
  });
}

export function getAnimalLookups(accessToken: string, locale: string, farmId: string) {
  return apiFetch<AnimalLookups>(`${animalsBase(farmId)}/lookups`, { accessToken, locale });
}

export function getFarmAnimal(accessToken: string, locale: string, farmId: string, animalId: string) {
  return apiFetch<FarmAnimal>(`${animalsBase(farmId)}/${animalId}`, { accessToken, locale });
}

export function createFarmAnimal(accessToken: string, locale: string, farmId: string, body: AnimalInput) {
  return apiFetch<FarmAnimal>(animalsBase(farmId), { accessToken, locale, method: 'POST', body });
}

export function updateFarmAnimal(
  accessToken: string,
  locale: string,
  farmId: string,
  animalId: string,
  body: AnimalInput,
) {
  return apiFetch<FarmAnimal>(`${animalsBase(farmId)}/${animalId}`, {
    accessToken,
    locale,
    method: 'PUT',
    body,
  });
}

export function archiveFarmAnimal(accessToken: string, locale: string, farmId: string, animalId: string) {
  return apiFetch<void>(`${animalsBase(farmId)}/${animalId}`, {
    accessToken,
    locale,
    method: 'DELETE',
  });
}

export function summaryRange(page: number, size: number, total: number) {
  if (total <= 0) {
    return { start: 0, end: 0 };
  }
  const start = page * size + 1;
  const end = Math.min((page + 1) * size, total);
  return { start, end };
}

export function paginationItems(current: number, totalPages: number): Array<number | 'ellipsis'> {
  if (totalPages <= 7) {
    return Array.from({ length: Math.max(totalPages, 0) }, (_, index) => index);
  }
  const items: Array<number | 'ellipsis'> = [0];
  const start = Math.max(1, current - 1);
  const end = Math.min(totalPages - 2, current + 1);
  if (start > 1) {
    items.push('ellipsis');
  }
  for (let page = start; page <= end; page += 1) {
    items.push(page);
  }
  if (end < totalPages - 2) {
    items.push('ellipsis');
  }
  items.push(totalPages - 1);
  return items;
}
