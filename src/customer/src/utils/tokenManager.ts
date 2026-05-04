import { STORAGE_KEYS } from './constants'

/**
 * Encapsulated localStorage access for auth tokens.
 * All other modules MUST use this — never access localStorage directly.
 * All methods are wrapped in try-catch (localStorage can throw in private browsing).
 */
export const tokenManager = {
  getAccessToken(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    } catch {
      return null
    }
  },

  setAccessToken(token: string): void {
    try {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token)
    } catch {
      // Silently fail — the app degrades to showing login
    }
  },

  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
    } catch {
      return null
    }
  },

  setRefreshToken(token: string): void {
    try {
      localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, token)
    } catch {
      // Silently fail
    }
  },

  getExpiresAt(): number | null {
    try {
      const value = localStorage.getItem(STORAGE_KEYS.EXPIRES_AT)
      return value ? Number(value) : null
    } catch {
      return null
    }
  },

  setExpiresAt(expiresAt: number): void {
    try {
      localStorage.setItem(STORAGE_KEYS.EXPIRES_AT, String(expiresAt))
    } catch {
      // Silently fail
    }
  },

  getRememberedIdentifier(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEYS.REMEMBERED_IDENTIFIER)
    } catch {
      return null
    }
  },

  setRememberedIdentifier(identifier: string): void {
    try {
      localStorage.setItem(STORAGE_KEYS.REMEMBERED_IDENTIFIER, identifier)
    } catch {
      // Silently fail
    }
  },

  clearRememberedIdentifier(): void {
    try {
      localStorage.removeItem(STORAGE_KEYS.REMEMBERED_IDENTIFIER)
    } catch {
      // Silently fail
    }
  },

  getLocale(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEYS.LOCALE)
    } catch {
      return null
    }
  },

  setLocale(locale: string): void {
    try {
      localStorage.setItem(STORAGE_KEYS.LOCALE, locale)
    } catch {
      // Silently fail
    }
  },

  clearAll(): void {
    try {
      localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.EXPIRES_AT)
    } catch {
      // Silently fail
    }
  },
}
