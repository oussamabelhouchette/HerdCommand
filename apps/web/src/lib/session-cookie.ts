export function hasAuthJsSessionCookie(names: string[]) {
  return names.some(
    (name) =>
      name === 'authjs.session-token' ||
      name.startsWith('authjs.session-token.') ||
      name === '__Secure-authjs.session-token' ||
      name.startsWith('__Secure-authjs.session-token.'),
  );
}
