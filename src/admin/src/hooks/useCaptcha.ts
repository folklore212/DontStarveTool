import { useState, useCallback, useRef, useEffect } from 'react'

export interface CaptchaParams {
  captchaOutput: string
  lotNumber: string
  passToken: string
  genTime: string
}

type FailMode = 'open' | 'closed'

const DEV_PARAMS: CaptchaParams = {
  captchaOutput: 'dev', lotNumber: 'dev', passToken: 'dev', genTime: '1',
}

interface UseCaptchaReturn {
  captchaParams: CaptchaParams | null
  isReady: boolean
  isLoading: boolean
  handleCaptchaReady: (params: CaptchaParams) => void
  reset: () => void
}

export default function useCaptcha(failMode: FailMode): UseCaptchaReturn {
  const isDev = import.meta.env.VITE_SKIP_CAPTCHA === 'true'
  const [captchaParams, setCaptchaParams] = useState<CaptchaParams | null>(isDev ? DEV_PARAMS : null)
  const [isReady, setIsReady] = useState(isDev)
  const [isLoading, setIsLoading] = useState(false)
  const sdkTimeout = useRef<ReturnType<typeof setTimeout>>()

  const handleCaptchaReady = useCallback((params: CaptchaParams) => {
    setCaptchaParams(params)
    setIsReady(true)
    setIsLoading(false)
    if (sdkTimeout.current) clearTimeout(sdkTimeout.current)
  }, [])

  const reset = useCallback(() => {
    setCaptchaParams(null)
    setIsReady(false)
    setIsLoading(false)
    if (sdkTimeout.current) clearTimeout(sdkTimeout.current)
    if (isDev) {
      setCaptchaParams(DEV_PARAMS)
      setIsReady(true)
      return
    }
    setIsLoading(true)
    sdkTimeout.current = setTimeout(() => {
      setIsLoading(false)
      if (failMode === 'closed') {
        setCaptchaParams(null)
      } else {
        setIsReady(true)
      }
    }, 10000)
  }, [failMode, isDev])

  useEffect(() => () => { if (sdkTimeout.current) clearTimeout(sdkTimeout.current) }, [])

  return { captchaParams, isReady, isLoading, handleCaptchaReady, reset }
}
