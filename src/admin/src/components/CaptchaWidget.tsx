import { useEffect, useRef, useImperativeHandle, forwardRef } from 'react'
import type { CaptchaParams } from '../hooks/useCaptcha'

interface CaptchaWidgetProps {
  captchaId: string
  failMode: 'open' | 'closed'
  onCaptchaReady: (params: CaptchaParams) => void
  onError?: (error: string) => void
}

export interface CaptchaWidgetRef {
  reset: () => void
  showCaptcha: () => void
}

const CAPTCHA_CONTAINER_ID = 'geetest-box'

interface GeeTestCaptchaObj {
  appendTo: (selector: string) => void
  getValidate: () => { captcha_output: string; lot_number: string; pass_token: string; gen_time: string } | null
  onSuccess: (cb: () => void) => void
  onError: (cb: (err: { code: string; msg: string }) => void) => void
  showCaptcha: () => void
  reset: () => void
}

declare const initGeetest4: ((config: { captchaId: string; product: string }, cb: (captchaObj: GeeTestCaptchaObj) => void) => void) | undefined

const CaptchaWidget = forwardRef<CaptchaWidgetRef, CaptchaWidgetProps>(
  function CaptchaWidget({ captchaId, failMode, onCaptchaReady, onError }, ref) {
    const captchaObjRef = useRef<GeeTestCaptchaObj | null>(null)
    const initialized = useRef(false)
    const onReadyRef = useRef(onCaptchaReady)
    onReadyRef.current = onCaptchaReady
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
        onReadyRef.current({ captchaOutput: 'dev', lotNumber: 'dev', passToken: 'dev', genTime: '1' })
        return
      }

      if (typeof initGeetest4 !== 'function') {
        if (failModeRef.current === 'open') {
          onReadyRef.current({ captchaOutput: '', lotNumber: '', passToken: '', genTime: '' })
        } else {
          onError?.('Captcha unavailable')
        }
        return
      }

      initGeetest4({ captchaId, product: 'bind' }, function onInit(captchaObj: GeeTestCaptchaObj) {
        captchaObjRef.current = captchaObj
        captchaObj.appendTo('#' + CAPTCHA_CONTAINER_ID)
        captchaObj.onSuccess(() => {
          const result = captchaObj.getValidate()
          if (result) {
            onReadyRef.current({
              captchaOutput: result.captcha_output,
              lotNumber: result.lot_number,
              passToken: result.pass_token,
              genTime: result.gen_time,
            })
          }
        })
        captchaObj.onError(() => {
          if (failModeRef.current === 'open') {
            onReadyRef.current({ captchaOutput: '', lotNumber: '', passToken: '', genTime: '' })
          } else {
            onError?.('Captcha failed')
          }
        })
      })
    }

    useEffect(() => {
      if (!initialized.current) {
        initialized.current = true
        initCaptcha()
      }
      return () => { captchaObjRef.current = null }
    }, [captchaId])

    return <div id={CAPTCHA_CONTAINER_ID} style={{ marginBottom: 16 }} />
  },
)

export default CaptchaWidget
