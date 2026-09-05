import { getLocale } from 'next-intl/server';
import { Link } from '@/i18n/navigation';

export default async function NotFound() {
  const locale = await getLocale();

  return (
    <div className="hc-shell">
      <main>
        <section className="hc-login">
          <h1>404</h1>
          <p>
            <Link href="/" locale={locale === 'en' ? 'en' : 'ar'}>
              HerdCommand
            </Link>
          </p>
        </section>
      </main>
    </div>
  );
}
