import client from './client'
import type { R } from '../types/api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  SendCodeRequest,
  VerifyCodeRequest,
  ResetPasswordRequest,
  RefreshTokenRequest,
  TokenValidationResponse,
} from '../types/auth'

/**
 * POST /auth/login
 * Login with identifier + credential + optional MFA code + captcha.
 * Returns LoginResponse — may have mfaRequired=true for two-step flow.
 *
 * Possible error codes: 10002, 10006, 10007, 10008, 10009, 10010
 */
export function login(data: LoginRequest): Promise<R<LoginResponse>> {
  return client.post('/auth/login', data).then((res) => res.data)
}

/**
 * POST /auth/register
 * Register a new user account.
 *
 * Possible error codes: 40002, 40003, 40004, 40050, 40051
 */
export function register(data: RegisterRequest): Promise<R<void>> {
  return client.post('/auth/register', data).then((res) => res.data)
}

/**
 * POST /auth/code/send
 * Send verification code to email/phone. Requires captcha.
 *
 * Possible error codes: 10010, 40001
 */
export function sendCode(data: SendCodeRequest): Promise<R<void>> {
  return client.post('/auth/code/send', data).then((res) => res.data)
}

/**
 * POST /auth/code/verify
 * Verify a verification code.
 *
 * Possible error codes: 40050, 40051
 */
export function verifyCode(data: VerifyCodeRequest): Promise<R<boolean>> {
  return client.post('/auth/code/verify', data).then((res) => res.data)
}

/**
 * POST /auth/password/reset
 * Reset password using a verified code.
 *
 * Possible error codes: 40060, 40061
 */
export function resetPassword(data: ResetPasswordRequest): Promise<R<void>> {
  return client.post('/auth/password/reset', data).then((res) => res.data)
}

/**
 * POST /auth/password/change
 * Change password (requires current password).
 */
export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<R<void>> {
  return client.post('/auth/password/change', data).then((res) => res.data)
}

/**
 * POST /auth/refresh
 * Refresh the access token using a refresh token.
 *
 * Possible error codes: 10003, 10004, 10011
 */
export function refreshToken(data: RefreshTokenRequest): Promise<R<LoginResponse>> {
  return client.post('/auth/refresh', data).then((res) => res.data)
}

/**
 * POST /auth/logout
 * Logout — invalidates the current session.
 */
export function logout(refreshToken?: string): Promise<R<void>> {
  return client.post('/auth/logout', refreshToken ? { refreshToken } : {}).then((res) => res.data)
}

/**
 * GET /auth/token/validate
 * Validate the current access token.
 */
export function validateToken(): Promise<R<TokenValidationResponse>> {
  return client.get('/auth/token/validate').then((res) => res.data)
}

/**
 * POST /auth/me/export
 * GDPR — Export all user data.
 */
export function exportData(): Promise<R<Record<string, unknown>>> {
  return client.post('/auth/me/export').then((res) => res.data)
}

/**
 * POST /auth/me/forget-me
 * GDPR — Request account deletion.
 */
export function forgetMe(): Promise<R<void>> {
  return client.post('/auth/me/forget-me').then((res) => res.data)
}

/**
 * GET /auth/captcha-config
 * Public endpoint — returns captcha IDs for frontend initialization.
 * No authentication required.
 */
export interface CaptchaConfig {
  loginCaptchaId: string
  registerCaptchaId: string
}

export function getCaptchaConfig(): Promise<R<CaptchaConfig>> {
  return client.get('/auth/captcha-config').then((res) => res.data)
}
