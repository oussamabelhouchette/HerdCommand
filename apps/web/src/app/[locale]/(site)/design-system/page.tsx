import { getTranslations, setRequestLocale } from 'next-intl/server';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';
import { Input } from '@/components/Input';

type Props = { params: Promise<{ locale: string }> };

export default async function DesignSystemPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations('components');

  return (
    <div className="hc-grid">
      <div className="hc-span-6">
        <Card title={t('button')}>
          <div className="hc-row">
            <Button>{t('primary')}</Button>
            <Button variant="secondary">{t('secondary')}</Button>
            <Button variant="destructive">{t('destructive')}</Button>
            <Button loading>{t('loading')}</Button>
            <Button disabled>{t('disabled')}</Button>
          </div>
        </Card>
      </div>
      <div className="hc-span-6">
        <Card title={t('input')}>
          <div className="hc-stack">
            <Input name="animal" label={t('label')} hint={t('hint')} />
            <Input name="animal-error" label={t('label')} error={t('errorMessage')} />
            <Input name="animal-disabled" label={t('label')} disabled />
          </div>
        </Card>
      </div>
      <div className="hc-span-12">
        <Card title={t('badge')}>
          <div className="hc-row">
            <Badge>{t('badge')}</Badge>
            <Badge tone="success">{t('success')}</Badge>
            <Badge tone="warning">{t('warning')}</Badge>
            <Badge tone="error">{t('error')}</Badge>
            <Badge tone="info">{t('info')}</Badge>
          </div>
        </Card>
      </div>
    </div>
  );
}
