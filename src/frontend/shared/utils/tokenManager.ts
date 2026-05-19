import { STORAGE_KEYS } from './constants'

export const tokenManager = {
  getAccessToken(): string | null {
    try { return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN) } catch { return null }
  },
  setAccessToken(token: string): void {
    try { localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token) } catch { /* degrade */ }
  },
  getRefreshToken(): string | null {
    try { return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN) } catch { return null }
  },
  setRefreshToken(token: string): void {
    try { localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, token) } catch { /* degrade */ }
  },
  getExpiresAt(): number | null {
    try { const v = localStorage.getItem(STORAGE_KEYS.EXPIRES_AT); return v ? Number(v) : null } catch { return null }
  },
  setExpiresAt(expiresAt: number): void {
    try { localStorage.setItem(STORAGE_KEYS.EXPIRES_AT, String(expiresAt)) } catch { /* degrade */ }
  },
  getLocale(): string | null {
    try { return localStorage.getItem(STORAGE_KEYS.LOCALE) } catch { return null }
  },
  clearAll(): void {
    try {
      localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.EXPIRES_AT)
    } catch { /* degrade */ }
  },
}
