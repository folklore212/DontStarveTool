import client from './client'
import type { R, MfaStatusVO, MfaSetupInitResponse } from '../types'

export function getMfaStatus() {
  return client.get<R<MfaStatusVO[]>>('/mfa/status')
}

export function getBackupCodes() {
  return client.get<R<string[]>>('/mfa/backup-codes')
}

export function setupInit(type: string) {
  return client.post<R<MfaSetupInitResponse>>('/mfa/setup/init', { mfaType: type })
}

export function setupVerify(code: string) {
  return client.post<R<null>>('/mfa/setup/verify', { code })
}

export function disableMfa(type: string, code: string) {
  return client.post<R<null>>('/mfa/disable', { mfaType: type, code })
}
