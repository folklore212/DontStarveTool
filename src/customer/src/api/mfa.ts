import client from './client'
import type { R } from '../types/api'

export interface MfaStatusVO {
  mfaType: string
  enabled: boolean
}

export interface MfaSetupInitResponse {
  secret: string
  qrCodeUri: string
  backupCodes: string[]
}

/** GET /mfa/status */
export function getMfaStatus(): Promise<R<MfaStatusVO[]>> {
  return client.get('/mfa/status').then((res) => res.data)
}

/** POST /mfa/setup/init */
export function initMfaSetup(mfaType: string): Promise<R<MfaSetupInitResponse>> {
  return client.post('/mfa/setup/init', { mfaType }).then((res) => res.data)
}

/** POST /mfa/setup/verify */
export function verifyMfaSetup(code: string): Promise<R<void>> {
  return client.post('/mfa/setup/verify', { code }).then((res) => res.data)
}

/** POST /mfa/disable */
export function disableMfa(password: string): Promise<R<void>> {
  return client.post('/mfa/disable', { password }).then((res) => res.data)
}

/** GET /mfa/backup-codes */
export function getBackupCodes(): Promise<R<string[]>> {
  return client.get('/mfa/backup-codes').then((res) => res.data)
}
