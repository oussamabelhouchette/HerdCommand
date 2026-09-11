import { getTranslations } from 'next-intl/server';
import { FarmNotice } from '@/components/farm/FarmNotice';

export default async function CurrentFarmNotFound() {
  const t = await getTranslations('farm');
  return <FarmNotice title={t('notFoundTitle')} body={t('notFoundHint')} />;
}
