import { getTranslations } from 'next-intl/server';
import { logoutAction } from '@/lib/logout';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';

type Props = {
  name: string;
  idToken?: string;
  locale?: string;
};

export async function FarmPortalCard({ name, idToken, locale }: Props) {
  const t = await getTranslations();

  return (
    <Card title={t('portal.title')}>
      <p>{name}</p>
      <p className="hc-muted">{t('portal.body')}</p>
      <form action={logoutAction}>
        {idToken ? <input type="hidden" name="idToken" value={idToken} /> : null}
        {locale ? <input type="hidden" name="locale" value={locale} /> : null}
        <Button type="submit" variant="secondary">
          {t('nav.signOut')}
        </Button>
      </form>
    </Card>
  );
}
