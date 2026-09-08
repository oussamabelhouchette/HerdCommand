import { apiFetch } from './api';

export type FeatureReleaseStatus = 'AVAILABLE' | 'COMING_SOON' | 'RETIRED';

export type CatalogFeature = {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  iconCode?: string | null;
  releaseStatus: FeatureReleaseStatus;
  enableable: boolean;
  displayOrder: number;
};

export type FeatureCatalog = {
  items: CatalogFeature[];
};

export function emptyFeatureCatalog(): FeatureCatalog {
  return { items: [] };
}

export function listFeatures(
  accessToken: string,
  locale: string,
  includeComingSoon = false,
) {
  return apiFetch<FeatureCatalog>('/api/v1/platform/features', {
    accessToken,
    locale,
    query: { includeComingSoon: includeComingSoon || undefined },
  });
}

/** Persist / compare selections by code, never by localized name. */
export function selectedFeatureCodes(features: CatalogFeature[]): string[] {
  return features.filter((feature) => feature.enableable).map((feature) => feature.code);
}

export function isComingSoon(feature: CatalogFeature): boolean {
  return feature.releaseStatus === 'COMING_SOON' || !feature.enableable;
}
