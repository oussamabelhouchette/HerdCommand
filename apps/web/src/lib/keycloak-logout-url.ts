export function buildKeycloakLogoutUrl(idToken?: string) {
  const issuer = process.env.AUTH_KEYCLOAK_ISSUER?.replace(/\/$/, '');
  const clientId = process.env.AUTH_KEYCLOAK_ID ?? 'herdcommand';
  const appUrl = process.env.NEXTAUTH_URL ?? 'http://localhost:3000';
  const postLogoutRedirectUri = `${appUrl}/login`;

  if (!issuer) {
    return `${appUrl}/login`;
  }

  const logoutUrl = new URL(`${issuer}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', clientId);
  logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  if (idToken) {
    logoutUrl.searchParams.set('id_token_hint', idToken);
  }
  return logoutUrl.toString();
}
