import { getTranslations, setRequestLocale } from 'next-intl/server';
import { Link } from '@/i18n/navigation';

type Props = { params: Promise<{ locale: string }> };

export default async function KeycloakLoginPreviewPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations('preview');

  return (
    <div className="hc-stack">
      <p className="hc-muted">{t('hint')}</p>
      <p>
        {/* Static files in public/kc-login-look, not App Router pages. */}
        {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
        <a href="/kc-login-look/">{t('open')}</a>
        {' · '}
        {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
        <a href="/kc-login-look/preview-en.html">{t('openEn')}</a>
      </p>
      <iframe
        title={t('title')}
        src={locale === 'en' ? '/kc-login-look/preview-en.html' : '/kc-login-look/'}
        style={{ width: '100%', minHeight: '900px', border: '1px solid var(--hc-color-border)', borderRadius: 16, background: '#f5f1eb' }}
      />
      <p>
        <Link href="/design-system">{t('back')}</Link>
      </p>
    </div>
  );
}
