import { useEffect, useRef, useImperativeHandle, forwardRef } from 'react'
import type { CaptchaParams } from '../types/auth'
import { DEV_CAPTCHA_PARAMS } from '../types/auth'
import { useTranslation } from '../i18n'
import { logger } from '../utils/logger'

interface CaptchaWidgetProps {
  captchaId: string
  onCaptchaReady: (params: CaptchaParams) => void
  onError?: (error: string) => void
  failMode: 'open' | 'closed'
}

export interface CaptchaWidgetRef {
  reset: () => void
  showCaptcha: () => void
}

const CAPTCHA_CONTAINER_ID = 'geetest-box'

const CaptchaWidget = forwardRef<CaptchaWidgetRef, CaptchaWidgetProps>(
  function CaptchaWidget({ captchaId, onCaptchaReady, onError, failMode }, ref) {
    const { t } = useTranslation()
    const captchaObjRef = useRef<GeeTestCaptchaObj | null>(null)
    const initialized = useRef(false)

    // Stable refs to avoid re-init
    const onCaptchaReadyRef = useRef(onCaptchaReady)
    onCaptchaReadyRef.current = onCaptchaReady
    const onErrorRef = useRef(onError)
    onErrorRef.current = onError
    const failModeRef = useRef(failMode)
    failModeRef.current = failMode

    useImperativeHandle(ref, () => ({
      reset() {
        captchaObjRef.current?.reset()
        initialized.current = false
        initCaptcha()
      },
      showCaptcha() {
        captchaObjRef.current?.showCaptcha()
      },
    }), [])

    function initCaptcha() {
      if (import.meta.env.VITE_SKIP_CAPTCHA === 'true') {
        onCaptchaReadyRef.current(DEV_CAPTCHA_PARAMS)
        return
      }

      if (typeof initGeetest4 !== 'function') {
        if (failModeRef.current === 'open') {
          onCaptchaReadyRef.current({ captchaOutput: '', lotNumber: '', passToken: '', genTime: '' })
        } else {
          onErrorRef.current?.('auth.error_captcha_unavailable')
        }
        return
      }

      const container = document.getElementById(CAPTCHA_CONTAINER_ID)
      if (!container) return

      try {
        initGeetest4({
          captchaId,
          product: 'bind',
        }, function onInit(captchaObj: GeeTestCaptchaObj) {
          captchaObjRef.current = captchaObj
          captchaObj.appendTo(`#${CAPTCHA_CONTAINER_ID}`)

          captchaObj.onSuccess(function () {
            const result = captchaObj.getValidate()
            if (result) {
              onCaptchaReadyRef.current({
                captchaOutput: result.captcha_output,
                lotNumber: result.lot_number,
                passToken: result.pass_token,
                genTime: result.gen_time,
              })
            }
          })

          captchaObj.onError(function (err: { code: string; msg: string }) {
            logger.error('GeeTest error', { code: err.code, msg: err.msg })
            if (failModeRef.current === 'open') {
              onCaptchaReadyRef.current({ captchaOutput: '', lotNumber: '', passToken: '', genTime: '' })
            } else {
              onErrorRef.current?.('auth.error_captcha_failed')
            }
          })
        })
      } catch (e) {
        logger.error('Failed to init GeeTest', { error: String(e) })
        if (failModeRef.current === 'open') {
          onCaptchaReadyRef.current({ captchaOutput: '', lotNumber: '', passToken: '', genTime: '' })
        } else {
          onErrorRef.current?.('auth.error_captcha_unavailable')
        }
      }
    }

    useEffect(() => {
      if (!initialized.current) {
        initialized.current = true
        initCaptcha()
      }
      return () => { captchaObjRef.current = null }
    }, [captchaId])

    if (import.meta.env.VITE_SKIP_CAPTCHA === 'true') {
      return (
        <div style={{ textAlign: 'center', marginBottom: 16, color: '#52c41a', fontSize: 13 }}>
          {t('auth.captcha_dev_bypass')}
        </div>
      )
    }

    return <div id={CAPTCHA_CONTAINER_ID} style={{ marginBottom: 16 }} />
  },
)

export default CaptchaWidget
