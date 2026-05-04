import { useState, useCallback } from 'react'
import { extractErrorMessage, ERROR_CODE_I18N_MAP, CODE_TO_FIELD_MAP } from '../utils/errorHandler'

interface UseFormErrorReturn {
  error: string | null
  fieldErrors: Record<string, string>
  setError: (msg: string) => void
  setFieldError: (field: string, msg: string) => void
  clearErrors: () => void
  parseApiError: (err: unknown) => string
  parseApiCode: (code: number) => string
}

/**
 * Centralizes form-level error state management.
 * Handles both global errors and per-field errors.
 */
export default function useFormError(): UseFormErrorReturn {
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const setFieldError = useCallback((field: string, msg: string) => {
    setFieldErrors((prev) => ({ ...prev, [field]: msg }))
  }, [])

  const clearErrors = useCallback(() => {
    setError(null)
    setFieldErrors({})
  }, [])

  const parseApiError = useCallback(
    (err: unknown): string => {
      const extracted = extractErrorMessage(err)
      const message = extracted.i18nKey || extracted.message

      if (extracted.field) {
        setFieldError(extracted.field, message)
      } else {
        setError(message)
      }

      return message
    },
    [setFieldError],
  )

  const parseApiCode = useCallback(
    (code: number): string => {
      const i18nKey = ERROR_CODE_I18N_MAP[code] ?? null
      const displayMsg = i18nKey || 'auth.error_unknown'
      const field = CODE_TO_FIELD_MAP[code] ?? null

      if (field) {
        setFieldError(field, displayMsg)
      } else {
        setError(displayMsg)
      }
      return displayMsg
    },
    [setFieldError],
  )

  return { error, fieldErrors, setError, setFieldError, clearErrors, parseApiError, parseApiCode }
}
