/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SKIP_CAPTCHA: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_GEETEST_LOGIN_CAPTCHA_ID: string
  readonly VITE_GEETEST_REGISTER_CAPTCHA_ID: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// GeeTest v4 global
interface GeeTestCaptchaObj {
  appendTo(selector: string): void
  getValidate(): {
    captcha_output: string
    lot_number: string
    pass_token: string
    gen_time: string
  }
  reset(): void
  showCaptcha(): void
  onSuccess(cb: () => void): void
  onError(cb: (err: { code: string; msg: string }) => void): void
  onReady(cb: () => void): void
  onClose(cb: () => void): void
}

interface GeeTestInitOptions {
  captchaId: string
  product?: 'bind' | 'popup' | 'float'
  language?: string
  onReady?: (captchaObj: GeeTestCaptchaObj) => void
  onSuccess?: (captchaObj: GeeTestCaptchaObj) => void
  onError?: (error: { code: string; msg: string }) => void
  onClose?: () => void
}

declare function initGeetest4(
  options: GeeTestInitOptions,
  callback?: (captchaObj: GeeTestCaptchaObj) => void,
): GeeTestCaptchaObj
