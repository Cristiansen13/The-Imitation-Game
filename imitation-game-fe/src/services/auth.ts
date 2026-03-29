// Custom JWT authentication — talks directly to the Python auth-service via Kong (/auth/*)

const AUTH_BASE = '/auth';

function parseErrorDetail(err: unknown): string | undefined {
  if (!err || typeof err !== 'object') return undefined;
  const detail = (err as Record<string, unknown>).detail;
  if (!detail) return undefined;
  if (typeof detail === 'string') return detail;
  if (Array.isArray(detail)) {
    return detail
      .map((e: unknown) => {
        if (typeof e === 'object' && e !== null) {
          const loc = (e as Record<string, unknown>).loc;
          const msg = (e as Record<string, unknown>).msg;
          const field = Array.isArray(loc) ? String(loc[loc.length - 1]) : '';
          return field ? `${field}: ${msg}` : String(msg);
        }
        return String(e);
      })
      .join('; ');
  }
  return String(detail);
}

interface TokenPayload {
  sub: string;
  preferred_username: string;
  email: string;
  name?: string;
  realm_access?: { roles: string[] };
  exp: number;
}

interface AuthTokens {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

function parseJwt(token: string): TokenPayload | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64)) as TokenPayload;
  } catch {
    return null;
  }
}

function saveTokens(tokens: AuthTokens): void {
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
}

function clearTokens(): void {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
}

export function getToken(): string | undefined {
  return localStorage.getItem('access_token') ?? undefined;
}

export function isAuthenticated(): boolean {
  const token = getToken();
  if (!token) return false;
  const payload = parseJwt(token);
  return payload != null && payload.exp * 1000 > Date.now();
}

export function getUserInfo() {
  const token = getToken();
  if (!token) return null;
  const payload = parseJwt(token);
  if (!payload) return null;
  return {
    id: payload.sub,
    username: payload.preferred_username,
    email: payload.email,
    name: payload.name ?? payload.preferred_username,
    roles: payload.realm_access?.roles ?? [],
  };
}

/** Called on app startup — silently restores session from refresh token. */
export async function initAuth(): Promise<boolean> {
  if (isAuthenticated()) {
    setupTokenRefresh();
    return true;
  }
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${AUTH_BASE}/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });
    if (!res.ok) { clearTokens(); return false; }
    const tokens: AuthTokens = await res.json();
    saveTokens(tokens);
    setupTokenRefresh();
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export async function login(username: string, password: string): Promise<void> {
  const res = await fetch(`${AUTH_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(parseErrorDetail(err) ?? 'Login failed');
  }
  const tokens: AuthTokens = await res.json();
  saveTokens(tokens);
  setupTokenRefresh();
}

export async function register(
  username: string,
  email: string,
  password: string,
): Promise<void> {
  const res = await fetch(`${AUTH_BASE}/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(parseErrorDetail(err) ?? 'Registration failed');
  }
}

export function logout(): void {
  clearTokens();
  if (_refreshTimer !== null) {
    clearInterval(_refreshTimer);
    _refreshTimer = null;
  }
}

let _refreshTimer: ReturnType<typeof setInterval> | null = null;

function setupTokenRefresh(): void {
  if (_refreshTimer !== null) clearInterval(_refreshTimer);
  // Check every minute; refresh if token expires within 2 minutes
  _refreshTimer = setInterval(async () => {
    const token = getToken();
    if (token) {
      const payload = parseJwt(token);
      if (payload && payload.exp * 1000 - Date.now() > 120_000) return;
    }
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) return;
    try {
      const res = await fetch(`${AUTH_BASE}/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: refreshToken }),
      });
      if (res.ok) {
        saveTokens(await res.json());
      } else {
        clearTokens();
      }
    } catch {
      // keep existing token until next check
    }
  }, 60_000);
}

export default { getToken, isAuthenticated, getUserInfo, initAuth, login, register, logout };
