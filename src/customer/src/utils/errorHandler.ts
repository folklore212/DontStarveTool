import { ErrorCode } from '../types/errors'
import type { ErrorCodeValue, ApiError } from '../types/errors'
import type { AxiosError } from 'axios'

/**
 * Maps backend ErrorCode values to i18n message keys.
 * Used by the error handler to produce user-facing messages.
 */
export const ERROR_CODE_I18N_MAP: Record<number, string> = {
  [ErrorCode.UNAUTHORIZED]: 'auth.error_session_expired',
  [ErrorCode.INVALID_CREDENTIALS]: 'auth.error_invalid_credentials',
  [ErrorCode.TOKEN_EXPIRED]: 'auth.error_session_expired',
  [ErrorCode.TOKEN_BLACKLISTED]: 'auth.error_session_expired',
  [ErrorCode.REFRESH_TOKEN_REPLAY]: 'auth.error_session_expired',
  [ErrorCode.MFA_REQUIRED]: 'auth.error_mfa_invalid',
  [ErrorCode.MFA_INVALID]: 'auth.error_mfa_invalid',
  [ErrorCode.ACCOUNT_LOCKED]: 'auth.error_account_locked',
  [ErrorCode.ACCOUNT_DISABLED]: 'auth.error_account_disabled',
  [ErrorCode.ACCOUNT_PENDING]: 'auth.error_account_pending',
  [ErrorCode.GEE_TEST_FAILED]: 'auth.error_captcha_failed',
  [ErrorCode.TOKEN_INVALID]: 'auth.error_session_expired',
  [ErrorCode.CAPTCHA_REQUIRED]: 'auth.error_captcha_required',
  [ErrorCode.VALIDATION_ERROR]: 'auth.error_unknown',
  [ErrorCode.USER_NOT_FOUND]: 'auth.error_user_not_found',
  [ErrorCode.USERNAME_EXISTS]: 'auth.error_username_exists',
  [ErrorCode.EMAIL_EXISTS]: 'auth.error_email_exists',
  [ErrorCode.PHONE_EXISTS]: 'auth.error_phone_exists',
  [ErrorCode.IDENTITY_TAKEN]: 'auth.error_identity_taken',
  [ErrorCode.LAST_IDENTITY]: 'auth.error_last_identity',
  [ErrorCode.USER_ALREADY_ACTIVATED]: 'auth.error_account_pending',
  [ErrorCode.ROLE_IN_USE]: 'auth.error_unknown',
  [ErrorCode.OAUTH_CLIENT_DISABLED]: 'auth.error_unknown',
  [ErrorCode.OAUTH_CODE_INVALID]: 'auth.error_invalid_credentials',
  [ErrorCode.VERIFICATION_CODE_INVALID]: 'auth.error_code_invalid',
  [ErrorCode.VERIFICATION_CODE_EXPIRED]: 'auth.error_code_expired',
  [ErrorCode.PASSWORD_REUSED]: 'auth.error_password_reused',
  [ErrorCode.PASSWORD_SAME]: 'auth.error_password_same',
  [ErrorCode.MFA_ALREADY_ENABLED]: 'auth.error_mfa_already_enabled',
  [ErrorCode.MFA_NOT_ENABLED]: 'auth.error_mfa_not_enabled',
  [ErrorCode.SERVICE_UNAVAILABLE]: 'common.service_unavailable',
  [ErrorCode.RATE_LIMITED]: 'auth.error_too_many_requests',
  [ErrorCode.INTERNAL_ERROR]: 'common.internal_error',
}

export interface ExtractedError {
  message: string // i18n key or raw message
  code: number
  i18nKey: string | null // null if no mapped key
  field: string | null // mapped form field name
  retryAfterSeconds: number | null // from 429 Retry-After header
  rawMessage: string // original server message
}

export const CODE_TO_FIELD_MAP: Record<number, string> = {
  [ErrorCode.USERNAME_EXISTS]: 'username',
  [ErrorCode.EMAIL_EXISTS]: 'email',
  [ErrorCode.PHONE_EXISTS]: 'phone',
  [ErrorCode.INVALID_CREDENTIALS]: 'password',
  [ErrorCode.VERIFICATION_CODE_INVALID]: 'code',
  [ErrorCode.VERIFICATION_CODE_EXPIRED]: 'code',
  [ErrorCode.MFA_INVALID]: 'mfaCode',
  [ErrorCode.PASSWORD_SAME]: 'newPassword',
  [ErrorCode.PASSWORD_REUSED]: 'newPassword',
  [ErrorCode.GEE_TEST_FAILED]: 'captcha',
  [ErrorCode.CAPTCHA_REQUIRED]: 'captcha',
}

/**
 * Extracts a structured error from any caught error value.
 * Handles Axios errors, network errors, timeout errors, and unknown types.
 */
export function extractErrorMessage(error: unknown): ExtractedError {
  if (isAxiosError(error)) {
    // No response — network error
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        return {
          message: 'auth.error_timeout',
          code: 0,
          i18nKey: 'auth.error_timeout',
          field: null,
          retryAfterSeconds: null,
          rawMessage: 'Request timed out',
        }
      }
      return {
        message: 'auth.error_network',
        code: 0,
        i18nKey: 'auth.error_network',
        field: null,
        retryAfterSeconds: null,
        rawMessage: error.message || 'Network error',
      }
    }

    const status = error.response.status
    const body = error.response.data as ApiError | undefined
    const code = body?.code ?? status
    const rawMessage = body?.message ?? ''

    // 429 — rate limit
    if (status === 429) {
      const retryAfter = error.response.headers['retry-after']
      const seconds = retryAfter ? parseInt(retryAfter, 10) : 60
      return {
        message: 'auth.error_rate_limited',
        code,
        i18nKey: 'auth.error_rate_limited',
        field: null,
        retryAfterSeconds: seconds,
        rawMessage: rawMessage || 'Rate limited',
      }
    }

    // 422 — validation with field errors
    if (status === 422 && body?.errors) {
      return {
        message: body.errors[0]?.message || rawMessage || 'Validation error',
        code,
        i18nKey: null,
        field: body.errors[0]?.field || null,
        retryAfterSeconds: null,
        rawMessage: body.errors[0]?.message || rawMessage,
      }
    }

    // Business error (400) or auth error (401)
    const i18nKey = code ? ERROR_CODE_I18N_MAP[code] ?? null : null
    const field = code ? CODE_TO_FIELD_MAP[code] ?? null : null

    return {
      message: i18nKey || 'auth.error_unknown',
      code: code || 0,
      i18nKey,
      field,
      retryAfterSeconds: null,
      rawMessage: rawMessage || 'Unknown error',
    }
  }

  // Not an axios error — likely a programming error
  if (error instanceof Error) {
    return {
      message: error.message,
      code: -1,
      i18nKey: null,
      field: null,
      retryAfterSeconds: null,
      rawMessage: error.message,
    }
  }

  return {
    message: 'auth.error_unknown',
    code: -1,
    i18nKey: 'auth.error_unknown',
    field: null,
    retryAfterSeconds: null,
    rawMessage: String(error),
  }
}

/**
 * Determines which form field an error code maps to.
 */
export function getErrorField(code: ErrorCodeValue): string | null {
  return CODE_TO_FIELD_MAP[code] ?? null
}

/**
 * Type guard for Axios errors.
 */
export function isAxiosError(error: unknown): error is AxiosError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'isAxiosError' in error &&
    (error as AxiosError).isAxiosError === true
  )
}
