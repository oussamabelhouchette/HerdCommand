import { apiFetch } from './api';

export type PlatformFoundation = {
  defaultLocale: string;
  supportedLocales: string[];
  permissions: string[];
};

export type PlatformFarmsProbe = {
  accessible: boolean;
  farms: unknown[];
};

export function emptyPlatformFoundation(): PlatformFoundation {
  return { defaultLocale: 'ar', supportedLocales: ['ar', 'en'], permissions: [] };
}

export function loadPlatformFoundation(accessToken: string, locale: string) {
  return apiFetch<PlatformFoundation>('/api/v1/platform', { accessToken, locale });
}

export function probePlatformFarms(accessToken: string, locale: string) {
  return apiFetch<PlatformFarmsProbe>('/api/v1/platform/farms', { accessToken, locale });
}
