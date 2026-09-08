import { routing } from '@/i18n/routing';

export function buildKeycloakLogoutUrl(idToken?: string, locale?: string) {
  const issuer = process.env.AUTH_KEYCLOAK_ISSUER?.replace(/\/$/, '');
  const clientId = process.env.AUTH_KEYCLOAK_ID ?? 'herdcommand';
  const appUrl = process.env.NEXTAUTH_URL ?? 'http://localhost:3000';
  const localePrefix =
    locale && locale !== routing.defaultLocale && routing.locales.includes(locale as 'ar' | 'en')
      ? `/${locale}`
      : '';
  const postLogoutRedirectUri = `${appUrl}${localePrefix}/login`;

  if (!issuer) {
    return postLogoutRedirectUri;
  }

  const logoutUrl = new URL(`${issuer}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', clientId);
  logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  if (idToken) {
    logoutUrl.searchParams.set('id_token_hint', idToken);
  }
  return logoutUrl.toString();
}
