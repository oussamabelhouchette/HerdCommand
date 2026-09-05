import { apiFetch } from './api';

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
  return { items: [], total: 0, page: 0, size: 20, sort: 'nameAr,asc' };
}

export function listFarms(accessToken: string, locale: string) {
  return apiFetch<Farm[]>('/api/v1/farms', { accessToken, locale });
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
      size: query.size ?? 20,
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
