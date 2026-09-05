import { getTranslations } from 'next-intl/server';
import { logoutAction } from '@/lib/logout';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';

type Props = {
  name: string;
};

export async function FarmPortalCard({ name }: Props) {
  const t = await getTranslations();

  return (
    <Card title={t('portal.title')}>
      <p>{name}</p>
      <p className="hc-muted">{t('portal.body')}</p>
      <form action={logoutAction}>
        <Button type="submit" variant="secondary">
          {t('nav.signOut')}
        </Button>
      </form>
    </Card>
  );
}
