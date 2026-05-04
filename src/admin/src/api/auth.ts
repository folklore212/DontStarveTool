import client from './client'
import type { R } from '../types'

export interface CaptchaConfig {
  loginCaptchaId: string
  registerCaptchaId: string
}

export function changePassword(oldPassword: string, newPassword: string) {
  return client.post('/auth/password/change', { oldPassword, newPassword })
}

export function getCaptchaConfig(): Promise<R<CaptchaConfig>> {
  return client.get('/auth/captcha-config').then((res) => res.data)
}
