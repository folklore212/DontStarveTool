import { useState, useCallback, useRef, useEffect } from 'react'
import type { CaptchaParams } from '../types/auth'
import { DEV_CAPTCHA_PARAMS } from '../types/auth'
import { TIMEOUTS } from '../utils/constants'
import { logger } from '../utils/logger'

type FailMode = 'open' | 'closed'

interface UseCaptchaReturn {
  captchaParams: CaptchaParams | null
  isReady: boolean
  isLoading: boolean
  error: string | null
  handleCaptchaReady: (params: CaptchaParams) => void
  reset: () => void
}

/**
 * Manages GeeTest captcha lifecycle.
 *
 * In dev mode (VITE_SKIP_CAPTCHA=true): immediately returns bypass params.
 * In prod mode: loads GeeTest v4 SDK, presents challenge, collects params.
 *
 * @param failMode 'open' = ignore captcha errors (login), 'closed' = require captcha (code-send)
 */
export default function useCaptcha(failMode: FailMode): UseCaptchaReturn {
  const [captchaParams, setCaptchaParams] = useState<CaptchaParams | null>(
    import.meta.env.VITE_SKIP_CAPTCHA === 'true' ? DEV_CAPTCHA_PARAMS : null,
  )
  const [isReady, setIsReady] = useState(
    import.meta.env.VITE_SKIP_CAPTCHA === 'true',
  )
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const sdkLoadTimeout = useRef<ReturnType<typeof setTimeout> | null>(null)

  const handleCaptchaReady = useCallback((params: CaptchaParams) => {
    setCaptchaParams(params)
    setIsReady(true)
    setIsLoading(false)
    if (sdkLoadTimeout.current) {
      clearTimeout(sdkLoadTimeout.current)
    }
  }, [])

  const reset = useCallback(() => {
    setCaptchaParams(null)
    setIsReady(false)
    setError(null)
    setIsLoading(false)
    if (sdkLoadTimeout.current) {
      clearTimeout(sdkLoadTimeout.current)
    }

    // In dev mode, immediately reset to bypass
    if (import.meta.env.VITE_SKIP_CAPTCHA === 'true') {
      setCaptchaParams(DEV_CAPTCHA_PARAMS)
      setIsReady(true)
      return
    }

    // In prod mode, would reload GeeTest SDK
    // This is a simplified implementation — actual GeeTest SDK integration
    // would use initGeetest4() from the GeeTest JS SDK
    setIsLoading(true)
    sdkLoadTimeout.current = setTimeout(() => {
      logger.error('Captcha SDK load timeout')
      setIsLoading(false)
      if (failMode === 'closed') {
        setError('auth.error_captcha_unavailable')
      } else {
        // Fail-open: ignore captcha failure
        setCaptchaParams(null)
        setIsReady(true)
      }
    }, TIMEOUTS.CAPTCHA_LOAD)
  }, [failMode])

  useEffect(() => {
    return () => {
      if (sdkLoadTimeout.current) {
        clearTimeout(sdkLoadTimeout.current)
      }
    }
  }, [])

  return { captchaParams, isReady, isLoading, error, handleCaptchaReady, reset }
}
