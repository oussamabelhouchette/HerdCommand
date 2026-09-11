import { apiFetch } from './api';

export const GROUP_PAGE_SIZE = 20;

export const GROUP_TYPE_CODES = ['DAMS', 'SIRES', 'YOUNG', 'FATTENING', 'ISOLATION', 'OTHER'] as const;
export type GroupTypeCode = (typeof GROUP_TYPE_CODES)[number];

/** Stored on every group until animal stories need real types. Hidden in the admin UI. */
export const DEFAULT_GROUP_TYPE: GroupTypeCode = 'OTHER';

export type Farm = {
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  active: boolean;
};

export type AnimalGroup = {
  id: string;
  farmId: string;
  code: string;
  nameAr: string;
  nameEn: string;
  groupTypeCode: GroupTypeCode;
  description: string | null;
  capacity: number | null;
  active: boolean;
  animalCount: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
};

export type GroupPage = {
  items: AnimalGroup[];
  total: number;
  page: number;
  size: number;
  sort: string;
};

export type GroupListQuery = {
  search?: string;
  groupTypeCode?: GroupTypeCode | '';
  active?: boolean | '';
  page?: number;
  size?: number;
  sort?: string;
};

export type GroupSearchState = GroupListQuery & {
  compose?: 'new' | 'edit';
  editId?: string;
  deactivateId?: string;
  formError?: string;
};

export type CreateGroupInput = {
  code: string;
  nameAr: string;
  nameEn: string;
  groupTypeCode: GroupTypeCode;
  description?: string;
  capacity?: number | null;
};

export type UpdateGroupInput = {
  nameAr: string;
  nameEn: string;
  groupTypeCode: GroupTypeCode;
  description?: string;
  capacity?: number | null;
};

export function emptyGroupPage(): GroupPage {
  return { items: [], total: 0, page: 0, size: GROUP_PAGE_SIZE, sort: 'nameAr,asc' };
}

function firstParam(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }
  return value ?? '';
}

export function parseGroupSearchParams(
  search: Record<string, string | string[] | undefined>,
): GroupSearchState {
  const pageValue = Number.parseInt(firstParam(search.page), 10);
  const activeRaw = firstParam(search.active);
  const editId = firstParam(search.edit) || undefined;
  return {
    search: firstParam(search.q).trim(),
    active: activeRaw === 'true' ? true : activeRaw === 'false' ? false : '',
    page: Number.isFinite(pageValue) && pageValue > 0 ? pageValue : 0,
    compose: editId ? 'edit' : firstParam(search.compose) === 'new' ? 'new' : undefined,
    editId,
    deactivateId: firstParam(search.deactivate) || undefined,
    formError: firstParam(search.error) || undefined,
  };
}

export function groupListQuery(query: GroupSearchState): GroupListQuery {
  return {
    search: query.search,
    active: query.active,
    page: query.page,
  };
}

export function groupsHref(
  farmId: string,
  query: GroupListQuery & { compose?: string; edit?: string; deactivate?: string; error?: string } = {},
) {
  const params = new URLSearchParams();
  if (query.search?.trim()) {
    params.set('q', query.search.trim());
  }
  if (query.active === true) {
    params.set('active', 'true');
  }
  if (query.active === false) {
    params.set('active', 'false');
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
  if (query.deactivate) {
    params.set('deactivate', query.deactivate);
  }
  if (query.error) {
    params.set('error', query.error);
  }
  const qs = params.toString();
  return qs ? `/farm/${farmId}/groups?${qs}` : `/farm/${farmId}/groups`;
}

export function normalizeGroupPage(value?: Partial<GroupPage> | null): GroupPage & { totalPages: number } {
  const empty = emptyGroupPage();
  const items = Array.isArray(value?.items) ? value.items : empty.items;
  const total = typeof value?.total === 'number' ? value.total : empty.total;
  const page = typeof value?.page === 'number' ? value.page : empty.page;
  const size = typeof value?.size === 'number' ? value.size : empty.size;
  return {
    items,
    total,
    page,
    size,
    sort: value?.sort ?? empty.sort,
    totalPages: total <= 0 || size <= 0 ? 0 : Math.ceil(total / size),
  };
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
  for (let next = start; next <= end; next += 1) {
    items.push(next);
  }
  if (end < totalPages - 2) {
    items.push('ellipsis');
  }
  items.push(totalPages - 1);
  return items;
}

export function listFarms(accessToken: string, locale: string) {
  return apiFetch<Farm[]>('/api/v1/farms', { accessToken, locale });
}

export function getGroup(accessToken: string, locale: string, farmId: string, groupId: string) {
  return apiFetch<AnimalGroup>(`/api/v1/farms/${farmId}/animal-groups/${groupId}`, { accessToken, locale });
}

export function listGroups(accessToken: string, locale: string, farmId: string, query: GroupListQuery = {}) {
  return apiFetch<GroupPage>(`/api/v1/farms/${farmId}/animal-groups`, {
    accessToken,
    locale,
    query: {
      search: query.search,
      groupTypeCode: query.groupTypeCode || undefined,
      active: query.active === '' || query.active === undefined ? undefined : query.active,
      page: query.page ?? 0,
      size: query.size ?? GROUP_PAGE_SIZE,
      sort: query.sort ?? 'nameAr,asc',
    },
  });
}

export function createGroup(accessToken: string, locale: string, farmId: string, body: CreateGroupInput) {
  return apiFetch<AnimalGroup>(`/api/v1/farms/${farmId}/animal-groups`, {
    accessToken,
    locale,
    method: 'POST',
    body,
  });
}

export function updateGroup(accessToken: string, locale: string, farmId: string, groupId: string, body: UpdateGroupInput) {
  return apiFetch<AnimalGroup>(`/api/v1/farms/${farmId}/animal-groups/${groupId}`, {
    accessToken,
    locale,
    method: 'PUT',
    body,
  });
}

export function updateGroupActive(accessToken: string, locale: string, farmId: string, groupId: string, active: boolean) {
  return apiFetch<AnimalGroup>(`/api/v1/farms/${farmId}/animal-groups/${groupId}/status`, {
    accessToken,
    locale,
    method: 'PATCH',
    body: { active },
  });
}
