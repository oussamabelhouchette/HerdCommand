import { apiFetch } from './api';

export const COLOR_TOKENS = ['success', 'purple', 'danger', 'warning', 'neutral'] as const;
export type ColorToken = (typeof COLOR_TOKENS)[number];

export const REQUIRED_STATUS_CODE = 'ACTIVE';

export type AnimalStatus = {
  code: string;
  labelAr: string;
  labelEn: string;
  colorToken: ColorToken;
  displayOrder: number;
  visibleInFilter: boolean;
  active: boolean;
  systemProtected: boolean;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
};

export type StatusPage = {
  items: AnimalStatus[];
  total: number;
  page: number;
  size: number;
  sort: string;
};

export type StatusListQuery = {
  search?: string;
  active?: boolean | '';
  page?: number;
  size?: number;
  sort?: string;
};

export type UpdateStatusInput = {
  labelAr: string;
  labelEn: string;
  colorToken: ColorToken;
  displayOrder: number;
  visibleInFilter: boolean;
  active: boolean;
};

const BASE = '/api/v1/admin/animal-statuses';

export function emptyStatusPage(): StatusPage {
  return { items: [], total: 0, page: 0, size: 20, sort: 'displayOrder,asc' };
}

export function listStatuses(accessToken: string, locale: string, query: StatusListQuery = {}) {
  return apiFetch<StatusPage>(BASE, {
    accessToken,
    locale,
    query: {
      search: query.search,
      active: query.active === '' || query.active === undefined ? undefined : query.active,
      page: query.page ?? 0,
      size: query.size ?? 20,
      sort: query.sort ?? 'displayOrder,asc',
    },
  });
}

export function updateStatus(accessToken: string, locale: string, code: string, body: UpdateStatusInput) {
  return apiFetch<AnimalStatus>(`${BASE}/${code}`, { accessToken, locale, method: 'PUT', body });
}

export function updateStatusActive(accessToken: string, locale: string, code: string, active: boolean) {
  return apiFetch<AnimalStatus>(`${BASE}/${code}/status`, {
    accessToken,
    locale,
    method: 'PATCH',
    body: { active },
  });
}

export function isRequiredStatus(code: string): boolean {
  return code.trim().toUpperCase() === REQUIRED_STATUS_CODE;
}

export function isAllowedColorToken(value: string): value is ColorToken {
  return (COLOR_TOKENS as readonly string[]).includes(value);
}

export function statusLabel(status: Pick<AnimalStatus, 'labelAr' | 'labelEn'>, locale: string): string {
  return locale.startsWith('en') ? status.labelEn : status.labelAr;
}
