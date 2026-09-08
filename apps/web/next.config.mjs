import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

const nextConfig = {
  transpilePackages: ['@herdcommand/tokens'],
  experimental: {
    optimizePackageImports: ['next-intl'],
    staleTimes: {
      dynamic: 30,
      static: 180,
    },
  },
  async headers() {
    return [
      {
        source: '/_next/static/:path*',
        headers: [{ key: 'Cache-Control', value: 'public, max-age=31536000, immutable' }],
      },
      {
        source: '/login',
        headers: [{ key: 'Cache-Control', value: 'public, max-age=60, stale-while-revalidate=300' }],
      },
      {
        source: '/en/login',
        headers: [{ key: 'Cache-Control', value: 'public, max-age=60, stale-while-revalidate=300' }],
      },
    ];
  },
};

export default withNextIntl(nextConfig);
