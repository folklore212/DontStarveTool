export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  EXPIRES_AT: 'expiresAt',
  REMEMBERED_IDENTIFIER: 'rememberedIdentifier',
  LOCALE: 'locale',
} as const

export const TIMEOUTS = {
  API_REQUEST: 10_000,
  TOKEN_REFRESH: 5_000,
} as const
