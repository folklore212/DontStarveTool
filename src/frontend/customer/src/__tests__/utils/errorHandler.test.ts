import { describe, it, expect } from 'vitest'
import { extractErrorMessage, getErrorField, isAxiosError, ERROR_CODE_I18N_MAP } from '../../utils/errorHandler'
import { ErrorCode } from '../../types/errors'
import type { ErrorCodeValue } from '../../types/errors'

describe('extractErrorMessage', () => {
  it('handles network errors (no response)', () => {
    const error = { isAxiosError: true, code: undefined, message: 'Network Error', response: undefined }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_network')
  })

  it('handles timeout errors', () => {
    const error = { isAxiosError: true, code: 'ECONNABORTED', message: 'timeout', response: undefined }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_timeout')
  })

  it('handles 429 rate limit with Retry-After header', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 429,
        headers: { 'retry-after': '60' },
        data: { code: 0, message: '' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_rate_limited')
    expect(result.retryAfterSeconds).toBe(60)
  })

  it('maps INVALID_CREDENTIALS to correct i18n key', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 401,
        data: { code: ErrorCode.INVALID_CREDENTIALS, message: 'Invalid credentials' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_invalid_credentials')
    expect(result.field).toBe('password')
  })

  it('maps ACCOUNT_LOCKED to correct i18n key', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 401,
        data: { code: ErrorCode.ACCOUNT_LOCKED, message: 'Account locked' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_account_locked')
  })

  it('maps field errors from 422 responses', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 422,
        data: {
          code: 11001,
          message: 'Validation error',
          errors: [{ field: 'email', message: 'Invalid email' }],
        },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.field).toBe('email')
  })

  it('handles plain Error objects', () => {
    const error = new Error('Something went wrong')
    const result = extractErrorMessage(error)
    expect(result.message).toContain('Something went wrong')
  })

  it('handles unknown error types', () => {
    const result = extractErrorMessage('just a string')
    expect(result.i18nKey).toBe('auth.error_unknown')
  })

  it('falls back to auth.error_unknown for unmapped error codes (never leaks raw message)', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        data: { code: 99999, message: 'some.server.message_key' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe(null)
    expect(result.message).toBe('auth.error_unknown')
    expect(result.rawMessage).toBe('some.server.message_key')
  })

  it('maps RATE_LIMITED to i18n key', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        data: { code: ErrorCode.RATE_LIMITED, message: 'system.rate_limited' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_too_many_requests')
  })

  it('maps CAPTCHA_REQUIRED to i18n key and captcha field', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        data: { code: ErrorCode.CAPTCHA_REQUIRED, message: 'auth.captcha_required' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_captcha_required')
    expect(result.field).toBe('captcha')
  })

  it('maps VERIFICATION_CODE_EXPIRED to i18n key', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        data: { code: ErrorCode.VERIFICATION_CODE_EXPIRED, message: 'verification.code_expired' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('auth.error_code_expired')
  })

  it('maps SERVICE_UNAVAILABLE to common.service_unavailable', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 503,
        data: { code: ErrorCode.SERVICE_UNAVAILABLE, message: 'system.service_unavailable' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('common.service_unavailable')
  })

  it('maps INTERNAL_ERROR to common.internal_error', () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 500,
        data: { code: ErrorCode.INTERNAL_ERROR, message: 'system.internal_error' },
      },
    }
    const result = extractErrorMessage(error)
    expect(result.i18nKey).toBe('common.internal_error')
  })
})

describe('getErrorField', () => {
  it('maps USERNAME_EXISTS to username field', () => {
    expect(getErrorField(ErrorCode.USERNAME_EXISTS)).toBe('username')
  })

  it('maps EMAIL_EXISTS to email field', () => {
    expect(getErrorField(ErrorCode.EMAIL_EXISTS)).toBe('email')
  })

  it('maps CAPTCHA_REQUIRED to captcha field', () => {
    expect(getErrorField(ErrorCode.CAPTCHA_REQUIRED)).toBe('captcha')
  })

  it('returns null for unknown codes', () => {
    expect(getErrorField(99999 as ErrorCodeValue)).toBe(null)
  })
})

describe('ERROR_CODE_I18N_MAP coverage', () => {
  it('has entries for all customer-facing error codes added in ErrorCode deduplication', () => {
    expect(ERROR_CODE_I18N_MAP[ErrorCode.TOKEN_INVALID]).toBeDefined()
    expect(ERROR_CODE_I18N_MAP[ErrorCode.CAPTCHA_REQUIRED]).toBeDefined()
    expect(ERROR_CODE_I18N_MAP[ErrorCode.USER_ALREADY_ACTIVATED]).toBeDefined()
    expect(ERROR_CODE_I18N_MAP[ErrorCode.RATE_LIMITED]).toBeDefined()
    expect(ERROR_CODE_I18N_MAP[ErrorCode.INTERNAL_ERROR]).toBeDefined()
    expect(ERROR_CODE_I18N_MAP[ErrorCode.SERVICE_UNAVAILABLE]).toBeDefined()
  })
})

describe('isAxiosError', () => {
  it('returns true for axios errors', () => {
    expect(isAxiosError({ isAxiosError: true })).toBe(true)
  })

  it('returns false for plain objects', () => {
    expect(isAxiosError({})).toBe(false)
  })

  it('returns false for null', () => {
    expect(isAxiosError(null)).toBe(false)
  })
})
