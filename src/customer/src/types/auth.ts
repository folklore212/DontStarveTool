// Auth-specific DTOs — mirrors backend DTOs exactly
// Field names match JSON keys sent to/received from the backend

export interface LoginRequest {
  identifier: string // username | email | phone
  credential: string // password
  mfaCode?: string
  captchaOutput?: string
  lotNumber?: string
  passToken?: string
  genTime?: string
}

export interface RegisterRequest {
  username: string // 3-64 chars, [a-zA-Z0-9_-]
  email: string // RFC 5322
  phone?: string
  password: string // 8-128 chars, meets complexity
  identityType: string // "email" | "phone"
  verificationCode: string // 6 digits
}

export interface SendCodeRequest {
  identifier: string
  identityType: string // "email" | "phone"
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'ACTIVATE'
  captchaOutput?: string
  lotNumber?: string
  passToken?: string
  genTime?: string
}

export interface VerifyCodeRequest {
  identifier: string
  code: string // 6 digits
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'ACTIVATE'
}

export interface ResetPasswordRequest {
  identifier: string
  code: string
  newPassword: string // meets PasswordComplexity
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface CaptchaParams {
  captchaOutput: string
  lotNumber: string
  passToken: string
  genTime: string
}

export const DEV_CAPTCHA_PARAMS: CaptchaParams = {
  captchaOutput: 'dev',
  lotNumber: 'dev',
  passToken: 'dev',
  genTime: '1',
}

// ---- Response DTOs ----

export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  expiresIn: number // seconds
  tokenType: 'Bearer'
  mfaRequired: boolean
  mfaTypes?: string[] // ["totp", "sms", "email"]
  newDevice: boolean
  userInfo?: UserInfo | null // null when mfaRequired=true
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string | null
  avatar: string | null
  permissions: string[]
  roles: string[]
}

export interface TokenValidationResponse {
  valid: boolean
  userId: number
  username: string
  permissions: string[]
  expiresAt: number
}
