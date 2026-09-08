import { getTranslations, setRequestLocale } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { ApiRequestError } from '@/lib/api';
import { requirePlatformAdmin } from '@/lib/require-platform-admin';
import { emptyFeatureCatalog, listFeatures } from '@/lib/features';
import {
  emptyFarmPage,
  emptyFarmSummary,
  farmListQueryFromSearchParams,
  getPlatformFarmSummary,
  listPlatformFarms,
} from '@/lib/farms';
import { loadPlatformFoundation } from '@/lib/platform';
import { FarmSettings } from '@/components/admin/FarmSettings';

type Search = Record<string, string | string[] | undefined>;

type Props = {
  params: Promise<{ locale: string }>;
  searchParams: Promise<Search>;
};

export default async function FarmSettingsPage({ params, searchParams }: Props) {
  const { locale } = await params;
  const query = farmListQueryFromSearchParams(await searchParams);
  setRequestLocale(locale);
  const gate = await requirePlatformAdmin(locale);
  const t = await getTranslations();

  if (!gate.allowed || !gate.session.accessToken) {
    return (
      <section className="hc-login">
        <h1>{t('admin.platformForbidden')}</h1>
        <p className="hc-muted">{t('admin.platformForbiddenHint')}</p>
        <p>
          <a href={getPathname({ href: '/portal', locale })}>{t('admin.backHome')}</a>
        </p>
      </section>
    );
  }

  const token = gate.session.accessToken;
  let apiReady = false;
  let loadError: string | undefined;
  let features = emptyFeatureCatalog();
  let initialPage = emptyFarmPage();
  let initialSummary = emptyFarmSummary();

  try {
    const [foundation, catalog, farmPage, summary] = await Promise.all([
      loadPlatformFoundation(token, locale),
      listFeatures(token, locale, true),
      listPlatformFarms(token, locale, query),
      getPlatformFarmSummary(token, locale),
    ]);
    apiReady = foundation.permissions.includes('PLATFORM_ADMIN');
    features = catalog;
    initialPage = farmPage;
    initialSummary = summary;
  } catch (error) {
    loadError = error instanceof ApiRequestError ? error.message : t('farmSettings.loadError');
  }

  return (
    <FarmSettings
      apiReady={apiReady}
      loadError={loadError}
      features={features.items}
      initialPage={initialPage}
      initialSummary={initialSummary}
      initialQuery={query}
    />
  );
}
