import type { UserInfo, CaptchaParams } from '../types/auth'
import type { ErrorCodeValue } from '../types/errors'
import { tokenManager } from '../utils/tokenManager'

// ---- State ----

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'mfa_pending' | 'error'

export interface MfaPendingContext {
  loginIdentifier: string
  credential: string // ONLY in memory during MFA step, cleared on unmount
  mfaTypes: string[]
  captchaParams: CaptchaParams | null
}

export interface AuthError {
  code: ErrorCodeValue | null
  message: string // user-facing i18n key
  params?: Record<string, string | number>
}

export interface AuthState {
  status: AuthStatus
  accessToken: string | null
  refreshToken: string | null
  expiresAt: number | null // epoch ms
  userInfo: UserInfo | null
  mfaContext: MfaPendingContext | null
  error: AuthError | null
}

// ---- Initial state ----

export function getInitialAuthState(): AuthState {
  const token = tokenManager.getAccessToken()
  const refreshToken = tokenManager.getRefreshToken()
  const expiresAt = tokenManager.getExpiresAt()

  if (token) {
    return {
      status: 'authenticated',
      accessToken: token,
      refreshToken: refreshToken || null,
      expiresAt: expiresAt ? Number(expiresAt) : null,
      userInfo: null, // Will be populated by first API call
      mfaContext: null,
      error: null,
    }
  }

  return {
    status: 'idle',
    accessToken: null,
    refreshToken: null,
    expiresAt: null,
    userInfo: null,
    mfaContext: null,
    error: null,
  }
}

// ---- Actions (discriminated union) ----

export type AuthAction =
  | { type: 'LOGIN_START' }
  | {
      type: 'LOGIN_MFA_REQUIRED'
      payload: MfaPendingContext
    }
  | {
      type: 'LOGIN_SUCCESS'
      payload: {
        accessToken: string
        refreshToken?: string
        expiresIn: number
        userInfo?: UserInfo | null
      }
    }
  | { type: 'LOGIN_FAILURE'; payload: AuthError }
  | { type: 'LOGOUT' }
  | {
      type: 'TOKEN_REFRESHED'
      payload: { accessToken: string; expiresIn: number }
    }
  | { type: 'CLEAR_ERROR' }
  | { type: 'SET_USER_INFO'; payload: UserInfo }

// ---- Reducer ----

export function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_START':
      return {
        ...state,
        status: 'loading',
        error: null,
        mfaContext: null, // Clear any stale MFA context
      }

    case 'LOGIN_MFA_REQUIRED':
      return {
        ...state,
        status: 'mfa_pending',
        mfaContext: action.payload,
        error: null,
      }

    case 'LOGIN_SUCCESS': {
      const { accessToken, refreshToken, expiresIn, userInfo } = action.payload
      tokenManager.setAccessToken(accessToken)
      if (refreshToken) tokenManager.setRefreshToken(refreshToken)
      const expiresAt = Date.now() + expiresIn * 1000
      tokenManager.setExpiresAt(expiresAt)
      return {
        ...state,
        status: 'authenticated',
        accessToken,
        refreshToken: refreshToken || null,
        expiresAt,
        userInfo: userInfo || null,
        mfaContext: null, // Clear credential from memory
        error: null,
      }
    }

    case 'LOGIN_FAILURE':
      return {
        ...state,
        status: 'error',
        error: action.payload,
        mfaContext: null, // Clear credential on failure too
      }

    case 'LOGOUT':
      tokenManager.clearAll()
      return {
        status: 'idle',
        accessToken: null,
        refreshToken: null,
        expiresAt: null,
        userInfo: null,
        mfaContext: null,
        error: null,
      }

    case 'TOKEN_REFRESHED': {
      const { accessToken, expiresIn } = action.payload
      tokenManager.setAccessToken(accessToken)
      const expiresAt = Date.now() + expiresIn * 1000
      tokenManager.setExpiresAt(expiresAt)
      return {
        ...state,
        accessToken,
        expiresAt,
      }
    }

    case 'CLEAR_ERROR':
      return {
        ...state,
        status: state.status === 'error' ? 'idle' : state.status,
        error: null,
      }

    case 'SET_USER_INFO':
      return {
        ...state,
        userInfo: action.payload,
      }

    default:
      return state
  }
}
