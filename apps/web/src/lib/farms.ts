import { apiFetch } from './api';

export const SEARCH_DEBOUNCE_MS = 400;
export const FARM_PAGE_SIZE = 20;
export const DEFAULT_FARM_SORT = 'createdAt,desc';

export const FARM_STATUSES = ['SETUP', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'] as const;
export const FARM_PLANS = ['TRIAL', 'ESSENTIAL', 'PROFESSIONAL'] as const;
export const FARM_SORTS = [
  'createdAt,desc',
  'createdAt,asc',
  'code,asc',
  'nameAr,asc',
  'nameEn,asc',
  'status,asc',
] as const;

export type FarmStatus = (typeof FARM_STATUSES)[number];
export type FarmPlan = (typeof FARM_PLANS)[number];
export type FarmSort = (typeof FARM_SORTS)[number];

export type PlatformFarmListItem = {
  id: string;
  code: string;
  name: string;
  ownerDisplayName?: string | null;
  ownerEmail?: string | null;
  planCode?: string | null;
  activeAnimalCount: number;
  maxActiveAnimals: number;
  enabledFeatureCodes: string[];
  status: string;
  createdAt: string;
  version: number;
};

export type PlatformFarmDetails = {
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
  ownerDisplayName?: string | null;
  ownerEmail?: string | null;
  ownerMembershipStatus?: string | null;
  planCode?: string | null;
  activeAnimalCount: number;
  maxActiveAnimals: number;
  maxTeamMembers?: number | null;
  trialEndsAt?: string | null;
  enabledFeatureCodes: string[];
  createdAt: string;
  createdBy?: string | null;
  version: number;
};

export type FarmPage = {
  items: PlatformFarmListItem[];
  total: number;
  page: number;
  size: number;
  sort: string;
};

export type FarmSummary = {
  totalFarms: number;
  activeFarms: number;
  setupFarms: number;
  suspendedFarms: number;
  archivedFarms: number;
  activeAnimals: number;
};

export type FarmListQuery = {
  search?: string;
  status?: FarmStatus | '';
  planCode?: FarmPlan | '';
  featureCode?: string;
  page?: number;
  size?: number;
  sort?: FarmSort | string;
};

export type SearchParamRecord = Record<string, string | string[] | undefined>;

const BASE = '/api/v1/platform/farms';

export function emptyFarmPage(): FarmPage {
  return { items: [], total: 0, page: 0, size: FARM_PAGE_SIZE, sort: DEFAULT_FARM_SORT };
}

export function emptyFarmSummary(): FarmSummary {
  return {
    totalFarms: 0,
    activeFarms: 0,
    setupFarms: 0,
    suspendedFarms: 0,
    archivedFarms: 0,
    activeAnimals: 0,
  };
}

export function listPlatformFarms(accessToken: string, locale: string, query: FarmListQuery = {}) {
  return apiFetch<FarmPage>(BASE, {
    accessToken,
    locale,
    query: {
      search: query.search?.trim() || undefined,
      status: query.status || undefined,
      planCode: query.planCode || undefined,
      featureCode: query.featureCode || undefined,
      page: query.page ?? 0,
      size: query.size ?? FARM_PAGE_SIZE,
      sort: query.sort || DEFAULT_FARM_SORT,
    },
  });
}

export function getPlatformFarmSummary(accessToken: string, locale: string) {
  return apiFetch<FarmSummary>(`${BASE}/summary`, { accessToken, locale });
}

export function getPlatformFarm(accessToken: string, locale: string, farmId: string) {
  return apiFetch<PlatformFarmDetails>(`${BASE}/${farmId}`, { accessToken, locale });
}

export function isFarmStatus(value: string): value is FarmStatus {
  return (FARM_STATUSES as readonly string[]).includes(value);
}

export function isFarmPlan(value: string): value is FarmPlan {
  return (FARM_PLANS as readonly string[]).includes(value);
}

export function isFarmSort(value: string): value is FarmSort {
  return (FARM_SORTS as readonly string[]).includes(value);
}

function firstParam(params: SearchParamRecord, key: string): string {
  const value = params[key];
  if (Array.isArray(value)) {
    return value[0]?.trim() ?? '';
  }
  return value?.trim() ?? '';
}

export function farmListQueryFromSearchParams(params: SearchParamRecord): FarmListQuery {
  const status = firstParam(params, 'status').toUpperCase();
  const planCode = firstParam(params, 'planCode').toUpperCase();
  const sort = firstParam(params, 'sort') || DEFAULT_FARM_SORT;
  const pageValue = Number.parseInt(firstParam(params, 'page'), 10);

  return {
    search: firstParam(params, 'search').slice(0, 200),
    status: isFarmStatus(status) ? status : '',
    planCode: isFarmPlan(planCode) ? planCode : '',
    featureCode: firstParam(params, 'featureCode'),
    page: Number.isFinite(pageValue) && pageValue > 0 ? pageValue : 0,
    size: FARM_PAGE_SIZE,
    sort: isFarmSort(sort) ? sort : DEFAULT_FARM_SORT,
  };
}

export function farmListSearchParams(query: FarmListQuery): Record<string, string> {
  const params: Record<string, string> = {};
  if (query.search?.trim()) {
    params.search = query.search.trim();
  }
  if (query.status) {
    params.status = query.status;
  }
  if (query.planCode) {
    params.planCode = query.planCode;
  }
  if (query.featureCode) {
    params.featureCode = query.featureCode;
  }
  if (query.page && query.page > 0) {
    params.page = String(query.page);
  }
  if (query.sort && query.sort !== DEFAULT_FARM_SORT) {
    params.sort = query.sort;
  }
  return params;
}

export function farmListHref(query: FarmListQuery): string {
  const qs = new URLSearchParams(farmListSearchParams(query)).toString();
  return qs ? `/admin/farm-settings?${qs}` : '/admin/farm-settings';
}
