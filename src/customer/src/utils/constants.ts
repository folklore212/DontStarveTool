/**
 * LocalStorage key constants — single source of truth.
 * Change keys here and all modules follow automatically.
 */
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  EXPIRES_AT: 'expiresAt',
  REMEMBERED_IDENTIFIER: 'rememberedIdentifier',
  LOCALE: 'locale',
} as const

/**
 * Timeout durations in milliseconds (unless noted).
 */
export const TIMEOUTS = {
  API_REQUEST: 10_000, // 10s
  CAPTCHA_LOAD: 15_000, // 15s
  TOKEN_REFRESH: 5_000, // 5s
  CODE_RESEND_COOLDOWN: 60, // seconds
  ERROR_AUTO_DISMISS: 8_000, // 8s
  SESSION_IDLE_WARNING: 840, // 14min (seconds, 1min before 15min token expiry)
  PASSWORD_STRENGTH_DEBOUNCE: 100, // ms
} as const

/**
 * Validation limits — mirrors backend constraints.
 */
export const LIMITS = {
  PASSWORD_MIN: 8,
  PASSWORD_MAX: 128,
  USERNAME_MIN: 3,
  USERNAME_MAX: 64,
  MFA_CODE_LENGTH: 6,
  BACKUP_CODE_LENGTH: 8,
  MAX_CODE_ATTEMPTS: 3,
  MAX_LOGIN_ATTEMPTS: 5, // matches backend lockout threshold
} as const

/**
 * Breakpoints in pixels — matches Ant Design 5 defaults.
 */
export const BREAKPOINTS = {
  XS: 480,
  SM: 576,
  MD: 768,
  LG: 992,
  XL: 1200,
  XXL: 1600,
} as const
