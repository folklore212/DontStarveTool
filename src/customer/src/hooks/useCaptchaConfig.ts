import { useState, useEffect } from 'react'
import { getCaptchaConfig } from '../api/auth'
import type { CaptchaConfig } from '../api/auth'
import { extractErrorMessage, ERROR_CODE_I18N_MAP } from '../utils/errorHandler'
import { logger } from '../utils/logger'

interface UseCaptchaConfigResult {
  loginCaptchaId: string | null
  registerCaptchaId: string | null
  loading: boolean
  error: string | null
}

// Module-level cache — config is session-stable, deduplicates requests across pages
let cachedPromise: Promise<CaptchaConfig> | null = null
let cachedConfig: CaptchaConfig | null = null

/**
 * Fetches captcha configuration from the backend at runtime.
 * Single source of truth — no compile-time VITE_GEETEST_* env vars needed.
 * Subsequent calls reuse the cached result.
 */
export default function useCaptchaConfig(): UseCaptchaConfigResult {
  const [config, setConfig] = useState<CaptchaConfig | null>(() => cachedConfig)
  const [loading, setLoading] = useState(!cachedConfig)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (cachedConfig) return

    let cancelled = false

    if (!cachedPromise) {
      cachedPromise = getCaptchaConfig().then((res) => {
        if (res.code !== 0) {
          throw new Error(ERROR_CODE_I18N_MAP[res.code] || 'auth.error_unknown')
        }
        return res.data
      })
    }

    cachedPromise
      .then((data) => {
        if (cancelled) return
        cachedConfig = data
        setConfig(data)
      })
      .catch((err) => {
        if (cancelled) return
        logger.error('Failed to load captcha config', err)
        const extracted = extractErrorMessage(err)
        setError(extracted.i18nKey || 'auth.error_unknown')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [])

  return {
    loginCaptchaId: config?.loginCaptchaId ?? null,
    registerCaptchaId: config?.registerCaptchaId ?? null,
    loading,
    error,
  }
}
