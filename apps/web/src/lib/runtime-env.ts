/** Read process.env at runtime. A dynamic key stops Next from inlining `undefined` at build. */
export function runtimeEnv(name: string): string | undefined {
  const value = process.env[name];
  return value ? value : undefined;
}
