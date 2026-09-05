import { getTranslations, setRequestLocale } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { ApiRequestError } from '@/lib/api';
import { requirePlatformAdmin } from '@/lib/require-platform-admin';
import { emptyFeatureCatalog, listFeatures } from '@/lib/features';
import { loadPlatformFoundation, probePlatformFarms } from '@/lib/platform';
import { FarmSettings } from '@/components/admin/FarmSettings';

type Props = { params: Promise<{ locale: string }> };

export default async function FarmSettingsPage({ params }: Props) {
  const { locale } = await params;
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

  try {
    const [foundation, farms, catalog] = await Promise.all([
      loadPlatformFoundation(token, locale),
      probePlatformFarms(token, locale),
      listFeatures(token, locale, true),
    ]);
    apiReady = foundation.permissions.includes('PLATFORM_ADMIN') && farms.accessible && farms.farms.length === 0;
    features = catalog;
  } catch (error) {
    loadError = error instanceof ApiRequestError ? error.message : t('farmSettings.loadError');
  }

  return <FarmSettings apiReady={apiReady} loadError={loadError} features={features.items} />;
}
