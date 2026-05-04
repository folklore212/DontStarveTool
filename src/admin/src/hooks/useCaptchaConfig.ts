import { useState, useEffect } from 'react'
import { getCaptchaConfig, type CaptchaConfig } from '../api/auth'

let cached: CaptchaConfig | null = null
let pending: Promise<CaptchaConfig> | null = null

export default function useCaptchaConfig() {
  const [captchaId, setCaptchaId] = useState<string | null>(() => cached?.loginCaptchaId ?? null)
  const [loading, setLoading] = useState(!cached)

  useEffect(() => {
    if (cached) return
    let cancelled = false
    if (!pending) {
      pending = getCaptchaConfig().then((res) => {
        if (res.code !== 0) throw new Error('Failed to load captcha config')
        return res.data
      })
    }
    pending.then((data) => {
      if (cancelled) return
      cached = data
      setCaptchaId(data.loginCaptchaId)
    }).catch(() => {}).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [])

  return { captchaId, loading }
}
