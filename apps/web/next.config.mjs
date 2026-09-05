import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

const nextConfig = {
  transpilePackages: ['@herdcommand/tokens'],
  experimental: {
    optimizePackageImports: ['next-intl'],
  },
};

export default withNextIntl(nextConfig);
