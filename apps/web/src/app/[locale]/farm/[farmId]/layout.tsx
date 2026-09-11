import { getTranslations, setRequestLocale } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { requireCurrentFarm } from '@/lib/farm-portal';
import { FarmNotice } from '@/components/farm/FarmNotice';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string; farmId: string }>;
};

export default async function CurrentFarmLayout({ children, params }: Props) {
  const { locale, farmId } = await params;
  setRequestLocale(locale);
  const current = await requireCurrentFarm(locale, farmId);

  if (!current.ok && current.code === 'forbidden') {
    return null;
  }
  if (!current.ok && current.code === 'load-error') {
    const t = await getTranslations('farm');
    return <FarmNotice title={t('loadError')} />;
  }
  if (!current.ok) {
    notFound();
  }

  return children;
}
