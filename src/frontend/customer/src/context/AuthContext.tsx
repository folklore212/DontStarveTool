import {
  createContext,
  useContext,
  useReducer,
  useCallback,
  useEffect,
  useRef,
} from 'react'
import { useNavigate } from 'react-router-dom'
import type { LoginRequest, CaptchaParams, UserInfo } from '../types/auth'
import { login as loginApi, logout as logoutApi } from '../api/auth'
import { extractErrorMessage, ERROR_CODE_I18N_MAP } from '../utils/errorHandler'
import { logger } from '../utils/logger'
import { tokenManager } from '../utils/tokenManager'
import {
  authReducer,
  getInitialAuthState,
} from './AuthReducer'
import type {
  AuthState,
  AuthAction,
  MfaPendingContext,
  AuthError,
} from './AuthReducer'

// ---- Context type ----

interface AuthContextType {
  state: AuthState
  beginLogin: (credentials: LoginRequest) => Promise<void>
  completeMfa: (mfaCode: string) => Promise<void>
  storeLoginResult: (accessToken: string, refreshToken?: string, expiresIn?: number, userInfo?: UserInfo | null) => void
  logout: () => Promise<void>
  clearError: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

// ---- Provider ----

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, null, getInitialAuthState)
  const navigate = useNavigate()
  const logoutInProgress = useRef(false)

  // Listen for 401/403 events from the axios interceptor
  useEffect(() => {
    const handle401 = () => {
      dispatch({ type: 'LOGOUT' })
      navigate('/login')
    }
    const handle403 = () => {
      // In customer portal, 403 means insufficient permissions.
      // Since this is an auth-only SPA, redirect to login with an error message.
      dispatch({ type: 'LOGOUT' })
      navigate('/login')
    }

    window.addEventListener('auth:unauthorized', handle401)
    window.addEventListener('auth:forbidden', handle403)
    return () => {
      window.removeEventListener('auth:unauthorized', handle401)
      window.removeEventListener('auth:forbidden', handle403)
    }
  }, [navigate])

  /**
   * Step 1 of login flow — call POST /auth/login.
   * If mfaRequired=true, transitions to mfa_pending state.
   */
  const beginLogin = useCallback(async (credentials: LoginRequest) => {
    dispatch({ type: 'LOGIN_START' })

    try {
      const response = await loginApi(credentials)

      if (response.code !== 0) {
        const i18nKey = ERROR_CODE_I18N_MAP[response.code] ?? null
        const error: AuthError = {
          code: response.code as AuthError['code'],
          message: i18nKey || 'auth.error_unknown',
        }
        dispatch({ type: 'LOGIN_FAILURE', payload: error })
        return
      }

      const data = response.data

      if (data.mfaRequired) {
        const captchaParams: CaptchaParams | null = credentials.captchaOutput
          ? {
              captchaOutput: credentials.captchaOutput,
              lotNumber: credentials.lotNumber || '',
              passToken: credentials.passToken || '',
              genTime: credentials.genTime || '',
            }
          : null

        const mfaContext: MfaPendingContext = {
          loginIdentifier: credentials.identifier,
          credential: credentials.credential, // Only held in memory
          mfaTypes: data.mfaTypes || ['totp'],
          captchaParams,
        }
        dispatch({ type: 'LOGIN_MFA_REQUIRED', payload: mfaContext })
        navigate('/mfa-verify', { state: mfaContext })
        return
      }

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: {
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          expiresIn: data.expiresIn,
          userInfo: data.userInfo,
        },
      })
      navigate('/dashboard')
    } catch (error: unknown) {
      const extracted = extractErrorMessage(error)
      const authError: AuthError = {
        code: extracted.code as AuthError['code'],
        message: extracted.i18nKey || extracted.message,
        params: extracted.retryAfterSeconds ? { seconds: String(extracted.retryAfterSeconds) } : undefined,
      }
      dispatch({ type: 'LOGIN_FAILURE', payload: authError })
    }
  }, [navigate])

  /**
   * Step 2 of login flow — re-login with MFA code.
   * Reuses POST /auth/login with the stored credential + mfaCode.
   */
  const completeMfa = useCallback(async (mfaCode: string) => {
    if (state.status !== 'mfa_pending' || !state.mfaContext) {
      logger.error('completeMfa called in invalid state', { status: state.status })
      return
    }

    dispatch({ type: 'LOGIN_START' })

    try {
      const loginRequest: LoginRequest = {
        identifier: state.mfaContext.loginIdentifier,
        credential: state.mfaContext.credential,
        mfaCode,
        ...(state.mfaContext.captchaParams || {}),
      }

      const response = await loginApi(loginRequest)

      if (response.code !== 0) {
        const i18nKey = ERROR_CODE_I18N_MAP[response.code] ?? null
        const error: AuthError = {
          code: response.code as AuthError['code'],
          message: i18nKey || 'auth.error_mfa_invalid',
        }
        dispatch({ type: 'LOGIN_FAILURE', payload: error })
        return
      }

      const data = response.data

      if (data.mfaRequired) {
        // Still requires MFA — shouldn't happen but handle gracefully
        const error: AuthError = {
          code: null,
          message: 'auth.error_mfa_invalid',
        }
        dispatch({ type: 'LOGIN_FAILURE', payload: error })
        return
      }

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: {
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          expiresIn: data.expiresIn,
          userInfo: data.userInfo,
        },
      })
      navigate('/dashboard')
    } catch (error: unknown) {
      const extracted = extractErrorMessage(error)
      const authError: AuthError = {
        code: extracted.code as AuthError['code'],
        message: extracted.i18nKey || extracted.message,
      }
      dispatch({ type: 'LOGIN_FAILURE', payload: authError })
    }
  }, [state.status, state.mfaContext, navigate])

  const logout = useCallback(async () => {
    if (logoutInProgress.current) return
    logoutInProgress.current = true

    try {
      try {
        const refreshToken = tokenManager.getRefreshToken()
        await logoutApi(refreshToken || undefined)
      } catch {
        // Server-side logout is best-effort
      }

      dispatch({ type: 'LOGOUT' })
      navigate('/login')
    } finally {
      logoutInProgress.current = false
    }
  }, [navigate])

  const clearError = useCallback(() => {
    dispatch({ type: 'CLEAR_ERROR' })
  }, [])

  const storeLoginResult = useCallback(
    (accessToken: string, refreshToken?: string, expiresIn?: number, userInfo?: UserInfo | null) => {
      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: {
          accessToken,
          refreshToken,
          expiresIn: expiresIn ?? 900,
          userInfo,
        },
      })
      navigate('/dashboard')
    },
    [navigate],
  )

  return (
    <AuthContext.Provider
      value={{ state, beginLogin, completeMfa, storeLoginResult, logout, clearError }}
    >
      {children}
    </AuthContext.Provider>
  )
}

// ---- Hook ----

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}

// Re-export for convenience
export type { AuthState, MfaPendingContext, AuthError, AuthAction }
