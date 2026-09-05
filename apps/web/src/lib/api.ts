export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiErrorBody = {
  code: string;
  message: string;
  fieldErrors?: ApiFieldError[];
  timestamp?: string;
  path?: string;
};

export class ApiRequestError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.message ?? `Request failed (${status})`);
    this.name = 'ApiRequestError';
    this.status = status;
    this.body = body;
  }
}

export function apiBaseUrl() {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
}

type Query = Record<string, string | number | boolean | undefined | null>;

export async function apiFetch<T>(
  path: string,
  options: {
    accessToken?: string;
    locale?: string;
    method?: string;
    body?: unknown;
    query?: Query;
    headers?: Record<string, string>;
  } = {},
): Promise<T> {
  const url = new URL(path.startsWith('http') ? path : `${apiBaseUrl()}${path}`);
  if (options.query) {
    for (const [key, value] of Object.entries(options.query)) {
      if (value === undefined || value === null || value === '') {
        continue;
      }
      url.searchParams.set(key, String(value));
    }
  }

  const headers: Record<string, string> = { ...options.headers };
  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }
  if (options.locale) {
    headers['Accept-Language'] = options.locale;
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    cache: 'no-store',
  });

  if (!response.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = null;
    }
    throw new ApiRequestError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export function fieldErrorsMap(error: unknown): Record<string, string> {
  if (!(error instanceof ApiRequestError) || !error.body?.fieldErrors) {
    return {};
  }
  const map: Record<string, string> = {};
  for (const item of error.body.fieldErrors) {
    map[item.field] = item.message;
  }
  return map;
}
