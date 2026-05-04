// User-related enum constants — mirrors backend Java enums

export const UserStatus = {
  NORMAL: 0,
  DISABLED: 1,
  PENDING: 2,
  LOCKED: 3,
} as const
export type UserStatusValue = (typeof UserStatus)[keyof typeof UserStatus]

export const IdentityType = {
  PHONE: 'phone',
  EMAIL: 'email',
  WECHAT: 'wechat',
  GITHUB: 'github',
  GOOGLE: 'google',
  APPLE: 'apple',
  USERNAME: 'username',
} as const
export type IdentityTypeValue = (typeof IdentityType)[keyof typeof IdentityType]

export const MfaType = {
  TOTP: 'totp',
  SMS: 'sms',
  EMAIL: 'email',
  WEBAUTHN: 'webauthn',
} as const
export type MfaTypeValue = (typeof MfaType)[keyof typeof MfaType]

export const AuthMethod = {
  PASSWORD: 'password',
  TOTP: 'totp',
  SMS: 'sms',
  OAUTH: 'oauth',
  API_KEY: 'api_key',
  SSO: 'sso',
} as const
export type AuthMethodValue = (typeof AuthMethod)[keyof typeof AuthMethod]

export const LoginResult = {
  SUCCESS: 'success',
  FAILED_CREDENTIAL: 'failed_credential',
  FAILED_MFA: 'failed_mfa',
  FAILED_LOCKED: 'failed_locked',
  FAILED_DISABLED: 'failed_disabled',
} as const
export type LoginResultValue = (typeof LoginResult)[keyof typeof LoginResult]
