import { getTranslations, setRequestLocale } from 'next-intl/server';
import { ApiRequestError } from '@/lib/api';
import { firstOwnerFarm } from '@/lib/owner-farms';
import { loadFarmPortal } from '@/lib/farm-portal';
import { redirect } from '@/i18n/navigation';
import { FarmNotice } from '@/components/farm/FarmNotice';

type Props = { params: Promise<{ locale: string }> };

export default async function FarmIndexPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations('farm');
  const portal = await loadFarmPortal(locale);

  if (!portal.gate.allowed || !portal.gate.session.accessToken) {
    return null;
  }
  if (portal.error) {
    const message = portal.error instanceof ApiRequestError ? portal.error.message : t('loadError');
    return <FarmNotice title={message} />;
  }

  const farm = firstOwnerFarm(portal.farms);
  if (farm) {
    redirect({ href: `/farm/${farm.id}`, locale });
    return null;
  }

  return <FarmNotice title={t('emptyTitle')} body={t('emptyHint')} />;
}
