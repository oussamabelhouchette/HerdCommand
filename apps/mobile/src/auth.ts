import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';
import * as SecureStore from 'expo-secure-store';

WebBrowser.maybeCompleteAuthSession();

const issuer = process.env.EXPO_PUBLIC_KEYCLOAK_ISSUER ?? 'http://localhost:9090/realms/herdcommand';
const clientId = process.env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID ?? 'herdcommand-mobile';
const redirectUri =
  process.env.EXPO_PUBLIC_REDIRECT_URI ?? AuthSession.makeRedirectUri({ scheme: 'herdcommand', path: 'auth' });

const discovery = {
  authorizationEndpoint: `${issuer}/protocol/openid-connect/auth`,
  tokenEndpoint: `${issuer}/protocol/openid-connect/token`,
  revocationEndpoint: `${issuer}/protocol/openid-connect/revoke`,
  endSessionEndpoint: `${issuer}/protocol/openid-connect/logout`,
};

export function useHerdCommandAuth() {
  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId,
      redirectUri,
      scopes: ['openid', 'profile', 'email'],
      usePKCE: true,
    },
    discovery,
  );

  return { request, response, promptAsync, redirectUri };
}

export async function persistTokens(accessToken: string) {
  await SecureStore.setItemAsync('hc.accessToken', accessToken);
}

export async function clearTokens() {
  await SecureStore.deleteItemAsync('hc.accessToken');
}

export { discovery, clientId, redirectUri };
