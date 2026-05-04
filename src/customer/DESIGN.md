# Customer-Facing Auth Frontend — Industrial-Grade Implementation Plan

## Context

Backend: Spring Boot 3.4.7 IAM system with 63 REST endpoints, JWT RS256 auth, GeeTest captcha, RBAC, MFA, GDPR. Existing admin SPA at `src/admin/` (React 18 + TS + Ant Design 5 + Vite 5). This plan creates a **new independent customer-facing auth portal** at `src/customer/` with mobile-first responsive design and industrial internet-grade robustness.

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│                  Presentation Layer                   │
│  pages/   — route-level page components              │
│  components/ — shared UI components                  │
├──────────────────────────────────────────────────────┤
│                 Application Layer                     │
│  context/ — AuthContext (state orchestration)        │
│  hooks/  — useCountdown, usePasswordStrength, etc.   │
├──────────────────────────────────────────────────────┤
│                 Infrastructure Layer                  │
│  api/    — HTTP client, endpoint functions           │
│  utils/  — tokenManager, errorHandler, validators    │
│  i18n/   — locale resources, useTranslation hook     │
├──────────────────────────────────────────────────────┤
│                    Type System                        │
│  types/  — All DTOs, enums, response wrappers        │
└──────────────────────────────────────────────────────┘
```

**Coupling rules:**
- Page components import from `api/`, `hooks/`, `components/`, `types/`, `i18n/` — never from other pages
- `components/` are pure UI, no API calls, no context — fully props-driven
- `api/` layer is stateless, no React imports, pure async functions
- `AuthContext` is the single source of truth for auth state — pages never read `localStorage` directly
- `utils/` are pure functions, no side effects, no React imports

## 2. Complete Directory Structure

```
src/customer/
  index.html                        # Entry HTML with CSP meta, viewport, noscript fallback
  package.json                      # Dependencies, scripts (dev/build/preview/lint/test)
  tsconfig.json                     # Strict TS config (mirrors admin)
  tsconfig.node.json                # For vite.config.ts
  vite.config.ts                    # Vite config: port 5173, proxy, build optimization
  .env.development                  # VITE_SKIP_CAPTCHA=true, VITE_API_BASE_URL=/api/v1
  .env.production                   # VITE_SKIP_CAPTCHA=false, VITE_API_BASE_URL=/api/v1
  .eslintrc.cjs                     # ESLint config
  .prettierrc                       # Prettier config
  vitest.config.ts                  # Test configuration
  
  src/
    main.tsx                         # ReactDOM.createRoot, providers bootstrap
    App.tsx                          # AppRoutes with route guards and Suspense boundaries
    vite-env.d.ts                    # Vite + import.meta.env type declarations
    
    types/
      index.ts                       # ALL TypeScript interfaces, enums, type aliases
      api.ts                         # R<T>, PageResult, PageQuery — API response wrappers
      auth.ts                        # Auth-specific DTOs (LoginRequest, RegisterRequest, etc.)
      user.ts                        # User-related types (UserVO, UserStatus enum, etc.)
      errors.ts                      # Error code constants, typed error map
    
    api/
      client.ts                      # Axios instance, interceptors, refresh queue
      auth.ts                        # Auth endpoint functions
      mfa.ts                         # MFA endpoint functions (if extended)
    
    context/
      AuthContext.tsx                 # AuthProvider + useAuth hook
      AuthReducer.ts                 # useReducer-based state machine
      authActions.ts                 # Action type definitions
      
    hooks/
      useCountdown.ts                # Resend code cooldown (generic, configurable)
      usePasswordStrength.ts         # Real-time password complexity evaluation
      useFormError.ts                # Form-level error state management
      useCaptcha.ts                  # GeeTest captcha lifecycle management
      useVerificationCode.ts         # Send/verify code orchestration
      useMfaFlow.ts                  # MFA two-step flow state machine
      useAutoFocus.ts                # Auto-focus traversal for code inputs
      useTranslation.ts              # i18n translator hook
      
    i18n/
      index.ts                       # useTranslation hook, LocaleProvider
      locales/
        zh-CN.ts                     # Chinese (Simplified) locale
        en.ts                        # English locale
      messages/
        zh-CN/
          common.json                # Shared labels (submit, cancel, loading...)
          auth.json                  # Auth page messages
          validation.json            # Validation error messages
        en/
          common.json
          auth.json
          validation.json
    
    pages/
      LoginPage.tsx
      RegisterPage.tsx
      ForgotPasswordPage.tsx
      EmailVerificationPage.tsx
      PasswordResetPage.tsx
      MfaVerifyPage.tsx
    
    components/
      AuthCard.tsx                   # Centered card layout wrapper
      PasswordStrengthBar.tsx        # Visual password complexity indicator (4-tier)
      CaptchaWidget.tsx              # GeeTest SDK wrapper with dev mode bypass
      VerificationCodeInput.tsx      # 6-digit segmented input with auto-advance
      CountdownButton.tsx            # Button with countdown cooldown
      LoadingOverlay.tsx             # Full-page spinner
      ErrorAlert.tsx                 # Dismissible error banner
      FormField.tsx                  # Enhanced Form.Item with error display
      PasswordInput.tsx              # Password field with show/hide + strength bar
      LanguageSwitcher.tsx           # Locale toggle (zh/en)
      NoScriptFallback.tsx           # <noscript> fallback content
      SuspenseFallback.tsx           # Lazy loading skeleton
    
    utils/
      tokenManager.ts                # Encapsulated localStorage access (single module)
      errorHandler.ts                # extractErrorMessage + ErrorCode → user message map
      passwordValidator.ts           # Client-side complexity calculator (mirrors backend)
      sanitize.ts                    # Input sanitization utilities
      constants.ts                   # Magic numbers: TIMEOUTS, TTLs, LIMITS
      logger.ts                      # Structured client-side logger (dev: console, prod: suppress PII)
    
    styles/
      global.css                     # Reset, font-family, CSS custom properties (design tokens)
      auth-card.css                  # AuthCard-specific styles
      responsive.css                 # Breakpoint definitions (320/768/1024/1440)
    
    __tests__/
      utils/
        passwordValidator.test.ts
        errorHandler.test.ts
        tokenManager.test.ts
      hooks/
        useCountdown.test.ts
        usePasswordStrength.test.ts
      components/
        PasswordStrengthBar.test.tsx
        VerificationCodeInput.test.tsx
        CaptchaWidget.test.tsx
      pages/
        LoginPage.test.tsx
        RegisterPage.test.tsx
      integration/
        auth-flow.test.tsx
```

## 3. Type System (`src/customer/src/types/`)

### `types/api.ts` — Generic API contracts
```ts
export interface R<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface FieldError {
  field: string
  message: string
}

export interface IPageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
```

### `types/auth.ts` — Auth-specific DTOs (mirrors backend DTOs exactly)
```ts
// Request DTOs — field names match backend JSON keys
export interface LoginRequest {
  identifier: string              // username | email | phone
  credential: string              // password
  mfaCode?: string
  captchaOutput?: string
  lotNumber?: string
  passToken?: string
  genTime?: string
}

export interface RegisterRequest {
  username: string                // 3-64 chars, [a-zA-Z0-9_-]
  email: string                   // RFC 5322
  phone?: string
  password: string                // 8-128 chars, meets complexity
  identityType: string            // "email" | "phone"
  verificationCode: string        // 6 digits
}

export interface SendCodeRequest {
  identifier: string
  identityType: string            // "email" | "phone"
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'ACTIVATE'
  captchaOutput?: string
  lotNumber?: string
  passToken?: string
  genTime?: string
}

export interface VerifyCodeRequest {
  identifier: string
  code: string                    // 6 digits
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'ACTIVATE'
}

export interface ResetPasswordRequest {
  identifier: string
  code: string
  newPassword: string             // meets @PasswordComplexity
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

// Response DTOs
export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  expiresIn: number               // seconds
  tokenType: 'Bearer'
  mfaRequired: boolean
  mfaTypes?: string[]             // ["totp", "sms", "email"]
  newDevice: boolean
  userInfo?: UserInfo | null      // null when mfaRequired=true
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string | null
  avatar: string | null
  permissions: string[]
}

export interface TokenValidationResponse {
  valid: boolean
  userId: number
  username: string
  permissions: string[]
  expiresAt: number
}
```

### `types/errors.ts` — Error code constants
```ts
// Mirrors backend ErrorCode.java enum
export const ErrorCode = {
  SUCCESS: 0,
  UNAUTHORIZED: 10001,
  INVALID_CREDENTIALS: 10002,
  TOKEN_EXPIRED: 10003,
  TOKEN_BLACKLISTED: 10004,
  MFA_REQUIRED: 10005,
  MFA_INVALID: 10006,
  ACCOUNT_LOCKED: 10007,
  ACCOUNT_DISABLED: 10008,
  ACCOUNT_PENDING: 10009,
  GEE_TEST_FAILED: 10010,
  REFRESH_TOKEN_REPLAY: 10011,
  VALIDATION_ERROR: 11001,
  USER_NOT_FOUND: 40001,
  USERNAME_EXISTS: 40002,
  EMAIL_EXISTS: 40003,
  PHONE_EXISTS: 40004,
  IDENTITY_TAKEN: 40005,
  VERIFICATION_CODE_INVALID: 40050,
  VERIFICATION_CODE_EXPIRED: 40051,
  PASSWORD_REUSED: 40060,
  PASSWORD_SAME: 40061,
  INTERNAL_ERROR: 50001,
  SERVICE_UNAVAILABLE: 50002,
} as const

export type ErrorCodeValue = (typeof ErrorCode)[keyof typeof ErrorCode]

export interface ApiError {
  code: ErrorCodeValue
  message: string
  timestamp: number
  errors?: FieldError[]           // for validation errors
}

// HTTP status constants
export const HttpStatus = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  TOO_MANY_REQUESTS: 429,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
} as const
```

### `types/user.ts` — User enums
```ts
// Mirrors backend enums
export const UserStatus = {
  NORMAL: 0,
  DISABLED: 1,
  PENDING: 2,
  LOCKED: 3,
} as const
export type UserStatusValue = typeof UserStatus[keyof typeof UserStatus]

export const IdentityType = {
  PHONE: 'phone',
  EMAIL: 'email',
  USERNAME: 'username',
} as const
export type IdentityTypeValue = typeof IdentityType[keyof typeof IdentityType]

export const MfaType = {
  TOTP: 'totp',
  SMS: 'sms',
  EMAIL: 'email',
  WEBAUTHN: 'webauthn',
} as const
export type MfaTypeValue = typeof MfaType[keyof typeof MfaType]
```

## 4. State Management — AuthContext with useReducer

Using `useReducer` instead of `useState` for predictable state transitions:

```ts
// AuthState — single source of truth
interface AuthState {
  status: 'idle' | 'loading' | 'authenticated' | 'mfa_pending' | 'error'
  accessToken: string | null
  refreshToken: string | null
  expiresAt: number | null          // epoch ms
  userInfo: UserInfo | null
  mfaContext: MfaPendingContext | null
  error: AuthError | null
}

// MfaPendingContext — only in memory, never localStorage
interface MfaPendingContext {
  loginIdentifier: string
  credential: string                // ONLY stored during MFA step, cleared after
  mfaTypes: string[]
  captchaParams: CaptchaParams | null
}

// Discriminated union for type-safe actions
type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_MFA_REQUIRED'; payload: MfaPendingContext }
  | { type: 'LOGIN_SUCCESS'; payload: { accessToken: string; refreshToken?: string; expiresIn: number; userInfo: UserInfo } }
  | { type: 'LOGIN_FAILURE'; payload: AuthError }
  | { type: 'LOGOUT' }
  | { type: 'TOKEN_REFRESHED'; payload: { accessToken: string; expiresIn: number } }
  | { type: 'MFA_VERIFY_START' }
  | { type: 'MFA_VERIFY_SUCCESS'; payload: { accessToken: string; refreshToken?: string; expiresIn: number; userInfo: UserInfo } }
  | { type: 'MFA_VERIFY_FAILURE'; payload: AuthError }
  | { type: 'CLEAR_ERROR' }

interface AuthError {
  code: ErrorCodeValue | null
  message: string                   // user-facing message
}

interface AuthContextType {
  state: AuthState
  login: (accessToken: string, refreshToken?: string) => void
  beginLogin: (credentials: LoginRequest) => Promise<void>
  completeMfa: (mfaCode: string) => Promise<void>
  logout: () => Promise<void>
  clearError: () => void
}
```

**State transition diagram:**
```
idle ──LOGIN_START──→ loading ──LOGIN_SUCCESS──→ authenticated
                        │
                        ├──LOGIN_MFA_REQUIRED──→ mfa_pending
                        │                            │
                        │              MFA_VERIFY_SUCCESS──→ authenticated
                        │              MFA_VERIFY_FAILURE──→ mfa_pending (retryable)
                        │
                        └──LOGIN_FAILURE──→ error ──CLEAR_ERROR──→ idle

authenticated ──LOGOUT──→ idle
authenticated ──TOKEN_REFRESHED──→ authenticated (update token)
```

**Invariants enforced by the reducer:**
- `credential` (password) exists ONLY in `mfa_pending` state, cleared on transition to any other state
- `accessToken` and `userInfo` are both present in `authenticated` or both absent in all other states
- Cannot transition from `idle` directly to `authenticated` without passing through `loading` or `mfa_pending`
- `LOGOUT` is idempotent — calling it from `idle` is a no-op

## 5. API Layer

### `api/client.ts` — Axios instance with full interceptor chain

```
Request interceptor:
  1. Read accessToken from tokenManager
  2. Attach Authorization: Bearer <token> header
  3. Attach X-Request-ID header (UUID for tracing)
  4. Log request start time for performance monitoring

Response interceptor (success):
  1. Log response time (ms)
  2. Return response.data (unwrap Axios response wrapper)

Response interceptor (error) — layered handling:
  Layer 1: Network error (!error.response)
    → logger.error('Network error', { url, method })
    → return Promise.reject(networkError)

  Layer 2: Timeout (error.code === 'ECONNABORTED')
    → logger.warn('Request timeout', { url, method, timeout: 10000 })
    → return Promise.reject(timeoutError)

  Layer 3: HTTP 401 — silent token refresh
    → Check: has refreshToken? AND NOT already refreshing? AND NOT a /auth/refresh request?
    → YES: enqueue request to refresh queue, perform single POST /auth/refresh
           → success: retry all queued requests with new token
           → failure: clear tokens, dispatch auth:unauthorized
    → NO:  clear tokens, dispatch auth:unauthorized

  Layer 4: HTTP 403
    → dispatch auth:forbidden (if not on login page)

  Layer 5: HTTP 429 — rate limit
    → Extract Retry-After header or use default
    → return Promise.reject(rateLimitError) with retryAfterSeconds

  Layer 6: HTTP 422 — validation error
    → Extract FieldError[] from response body
    → return Promise.reject(validationError) with field errors
  
  Layer 7: HTTP 400/500 — business/server error
    → Extract code + message from response body
    → Map ErrorCode to user-facing i18n message
    → return Promise.reject(businessError)
```

**Refresh queue implementation:**
```ts
let isRefreshing = false
let pendingQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

function subscribeToRefresh(resolve, reject) {
  pendingQueue.push({ resolve, reject })
}

function onRefreshed(newToken: string) {
  pendingQueue.forEach(({ resolve }) => resolve(newToken))
  pendingQueue = []
}

function onRefreshFailed(error: unknown) {
  pendingQueue.forEach(({ reject }) => reject(error))
  pendingQueue = []
}
```

### `api/auth.ts` — Typed API functions

Every function:
- Takes a typed request object
- Returns `Promise<R<T>>` where T is the specific response type
- Has JSDoc documenting the backend error codes that can be returned
- Handles no network-specific logic (that's in client.ts)

```ts
import client from './client'
import type { R } from '../types/api'
import type {
  LoginRequest, LoginResponse,
  RegisterRequest,
  SendCodeRequest,
  VerifyCodeRequest,
  ResetPasswordRequest,
  RefreshTokenRequest,
  TokenValidationResponse,
} from '../types/auth'

export function login(data: LoginRequest): Promise<R<LoginResponse>>
export function register(data: RegisterRequest): Promise<R<void>>
export function sendCode(data: SendCodeRequest): Promise<R<void>>
export function verifyCode(data: VerifyCodeRequest): Promise<R<boolean>>
export function resetPassword(data: ResetPasswordRequest): Promise<R<void>>
export function refreshToken(data: RefreshTokenRequest): Promise<R<LoginResponse>>
export function logout(refreshToken?: string): Promise<R<void>>
export function validateToken(): Promise<R<TokenValidationResponse>>
export function exportData(): Promise<R<Record<string, unknown>>>
export function forgetMe(): Promise<R<void>>
```

## 6. Routing Design

```tsx
// App.tsx
function AppRoutes() {
  const { state } = useAuth()
  const location = useLocation()

  // Authenticated users: redirect all auth pages to home
  if (state.status === 'authenticated') {
    return (
      <Routes>
        <Route path="/*" element={<Navigate to="/" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={
        <Suspense fallback={<SuspenseFallback />}>
          <LoginPage />
        </Suspense>
      } />
      <Route path="/register" element={
        <Suspense fallback={<SuspenseFallback />}>
          <RegisterPage />
        </Suspense>
      } />
      <Route path="/forgot-password" element={
        <Suspense fallback={<SuspenseFallback />}>
          <ForgotPasswordPage />
        </Suspense>
      } />
      <Route path="/verify-email" element={
        <RequireNavigationState requiredKeys={['identifier', 'purpose']}>
          <Suspense fallback={<SuspenseFallback />}>
            <EmailVerificationPage />
          </Suspense>
        </RequireNavigationState>
      } />
      <Route path="/mfa-verify" element={
        <RequireNavigationState requiredKeys={['mfaTypes', 'loginIdentifier', 'credential']}>
          <Suspense fallback={<SuspenseFallback />}>
            <MfaVerifyPage />
          </Suspense>
        </RequireNavigationState>
      } />
      <Route path="/reset-password" element={
        <RequireNavigationState requiredKeys={['identifier', 'code']}>
          <Suspense fallback={<SuspenseFallback />}>
            <PasswordResetPage />
          </Suspense>
        </RequireNavigationState>
      } />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

// Navigation state guard component
function RequireNavigationState({
  requiredKeys,
  children,
}: {
  requiredKeys: string[]
  children: React.ReactNode
}) {
  const location = useLocation()
  const hasState = requiredKeys.every(key => key in (location.state ?? {}))
  
  if (!hasState) {
    return <Navigate to="/login" replace />
  }
  
  return <>{children}</>
}
```

**Navigation state contracts:**

| Source → Target | `navigate(to, { state })` |
|---|---|
| LoginPage → MfaVerifyPage | `{ mfaTypes, loginIdentifier, credential, captchaParams }` |
| RegisterPage → EmailVerificationPage | `{ identifier, purpose: 'REGISTER', identityType }` |
| ForgotPasswordPage → EmailVerificationPage | `{ identifier, purpose: 'RESET_PASSWORD', identityType }` |
| EmailVerificationPage → PasswordResetPage | `{ identifier, code, purpose: 'RESET_PASSWORD' }` |
| EmailVerificationPage → LoginPage | no state needed (registration complete) |
| PasswordResetPage → LoginPage | no state needed + `message.success(...)` |
| MfaVerifyPage → / | login() called in AuthContext |

## 7. Component Specifications

### 7.1 `LoginPage`

**File:** `src/customer/src/pages/LoginPage.tsx`

```
┌─────────────────────────────────┐
│         AuthCard                 │
│  ┌───────────────────────────┐  │
│  │    LanguageSwitcher (zh/en)│  │
│  │                           │  │
│  │    App Logo / Title       │  │
│  │    "Welcome back" subtitle│  │
│  │                           │  │
│  │  ┌─ ErrorAlert (if any) ──┐│  │
│  │  └────────────────────────┘│  │
│  │                           │  │
│  │  [Identifier input      ] │  │
│  │  [Password input        ] │  │
│  │  [MFA code input (opt)  ] │  │
│  │  [Remember me checkbox  ] │  │
│  │                           │  │
│  │  [CaptchaWidget (GeeTest)]│  │
│  │                           │  │
│  │  [  Log in (loading)    ] │  │
│  │                           │  │
│  │  Forgot password?  │ Create account │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Form fields with validation rules:**

| Field | Component | Validation Rules |
|---|---|---|
| `identifier` | `<Input prefix={UserOutlined}>` | `required: true, minLength: 3, maxLength: 255, whitespace: true` (trims) |
| `password` | `<PasswordInput>` | `required: true, minLength: 8, maxLength: 128` |
| `mfaCode` | `<Input maxLength={6}>` | `pattern: /^\d{0,6}$/, len: 6` (only if visible) |
| `rememberMe` | `<Checkbox>` | none (optional) |

**Behavior:**
1. On mount: check `localStorage` for saved identifier (from "Remember me"), populate field

**GeeTest Captcha on Login (backend fail-open):**

The login form uses GeeTest captcha. The backend enforces **fail-open** — if GeeTest is unavailable, login proceeds without verification. Flow:

- Page mount: `CaptchaWidget` loads GeeTest v4 SDK asynchronously (non-blocking)
- User completes GeeTest challenge → `onCaptchaReady` returns `{ captchaOutput, lotNumber, passToken, genTime }`
- These 4 params are sent with the login request
- If GeeTest SDK load fails: login proceeds without captcha params (backend tolerates this on the login endpoint)
- If user skips captcha and GeeTest IS available: backend may return `GEE_TEST_FAILED (10010)`
- Dev mode: bypass params injected immediately, no popup
- On mount: captcha auto-refreshes (stale tokens discarded)

**Submit flow:**
2. Dispatch `LOGIN_START` → call `POST /auth/login` with `identifier`, `credential`, optional `mfaCode`, captcha params
3. On `mfaRequired === true`: dispatch `LOGIN_MFA_REQUIRED` with context, `navigate('/mfa-verify', { state: mfaContext })`
4. On success: dispatch `LOGIN_SUCCESS`, store tokens, save identifier if "Remember me" checked, `navigate('/')`
5. On specific error codes:
   - `ACCOUNT_LOCKED (10007)` → show "Account locked. Try again in X minutes." with the lockedUntil time
   - `ACCOUNT_DISABLED (10008)` → show "Account disabled. Contact support."
   - `ACCOUNT_PENDING (10009)` → show "Account not activated. Please verify your email." with resend link
   - `INVALID_CREDENTIALS (10002)` → show "Invalid username or password."
   - `GEE_TEST_FAILED (10010)` → show "Captcha verification failed. Please try again." + reload captcha
   - `MFA_INVALID (10006)` → show error on mfaCode field
6. On rate limit (HTTP 429) → show countdown via `Retry-After` header

**Edge cases:**
- Double-submit prevention: disable submit button while `state.status === 'loading'`
- Network offline: show offline banner, disable form
- Backend unreachable (timeout): show "Server unavailable. Please try again later." with retry button
- Stale captcha token: captcha widget auto-refreshes on mount

### 7.2 `RegisterPage`

**File:** `src/customer/src/pages/RegisterPage.tsx`

```
┌─────────────────────────────────┐
│         AuthCard                 │
│  ┌───────────────────────────┐  │
│  │    "Create Account" title │  │
│  │                           │  │
│  │  [Username input        ] │  │
│  │  [Email input           ] │  │
│  │  [Phone input (optional)] │  │
│  │  [Password input        ] │  │
│  │  PasswordStrengthBar     │  │
│  │  [Confirm password      ] │  │
│  │                           │  │
│  │  [CaptchaWidget]         │  │
│  │  [VerificationCodeInput  ]│  │
│  │                           │  │
│  │  [Agreement checkbox    ] │  │
│  │                           │  │
│  │  [  Create Account     ] │  │
│  │                           │  │
│  │  Already have account? Log in │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Form fields with validation rules:**

| Field | Rules |
|---|---|
| `username` | `required, minLength: 3, maxLength: 64, pattern: /^[a-zA-Z0-9_-]+$/` |
| `email` | `required, type: 'email', pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/` |
| `phone` | `pattern: /^\+?[0-9]{7,15}$/` (optional, international format) |
| `password` | `required, minLength: 8, maxLength: 128, custom: passwordComplexity` |
| `confirmPassword` | `required, dependencies: ['password'], custom: mustMatch` |
| `acceptTerms` | `required, validator: (_, value) => value ? Promise.resolve() : Promise.reject()` |

**`passwordComplexity` custom validator (synchronous):**
```ts
// Uses usePasswordStrength hook internally
// Evaluates against all 4 rules (length, character classes, username check, email check)
// Fails if score < 3 (backend requires 3 of 4)
```

**Behavior:**
1. `password` field changes trigger real-time `PasswordStrengthBar` update
2. `confirmPassword` validates on both its own change AND `password` change (Ant Design `dependencies`)
3. `VerificationCodeInput` component:
   - 6-digit segmented input with auto-focus advance
   - Code value is used in the final registration submit
   - "Send Code" button is disabled until email passes client-side validation

	**Send Verification Code Flow (GeeTest mandatory — backend fail-closed):**

	The "Send Code" button requires a successful GeeTest captcha before calling the API:

	1. User fills in email (and username/password for register)
	2. User clicks "Send Code" → `CaptchaWidget` loads GeeTest v4 SDK
	3. GeeTest challenge popup appears → user completes slider/puzzle verification
	4. `CaptchaWidget` calls `onCaptchaReady({ captchaOutput, lotNumber, passToken, genTime })`
	5. These 4 captcha params are merged into `POST /auth/code/send` request body
	6. On success: 60s cooldown starts, code input becomes active
	7. On captcha failure (`GEE_TEST_FAILED 10010`): show error "安全验证失败，请重试", captcha resets
	8. On GeeTest SDK load timeout (15s): show error "安全验证服务暂不可用，请稍后重试", disable button
	9. Dev mode (`VITE_SKIP_CAPTCHA=true`): `CaptchaWidget` immediately fires `onCaptchaReady` with bypass params `{ captchaOutput: 'dev', lotNumber: 'dev', passToken: 'dev', genTime: '1' }` — no popup
4. Submit:
   - Validate all fields client-side
   - Call `POST /auth/register`
   - On success: `navigate('/login')` with `message.success(t('auth.registration_success'))`
   - On `USERNAME_EXISTS (40002)`: show error on username field
   - On `EMAIL_EXISTS (40003)`: show error on email field
   - On `PHONE_EXISTS (40004)`: show error on phone field
   - On `VERIFICATION_CODE_INVALID (40050)`: show error on code input
   - On `VERIFICATION_CODE_EXPIRED (40051)`: show "Code expired. Request a new one."

**Edge cases:**
- Username availability: debounced check (future enhancement, not in v1)
- Email normalization: trim + lowercase before sending
- Password strength bar updates in real-time as user types

### 7.3 `ForgotPasswordPage`

**File:** `src/customer/src/pages/ForgotPasswordPage.tsx`

```
┌─────────────────────────────────┐
│         AuthCard                 │
│  ┌───────────────────────────┐  │
│  │    "Forgot Password"      │  │
│  │    "Enter email to get   │  │
│  │     reset code"          │  │
│  │                           │  │
│  │  [Email input           ] │  │
│  │                           │  │
│  │  [CaptchaWidget (GeeTest)]│  │
│  │                           │  │
│  │  [Send Code (cooldown)  ] │  │
│  │                           │  │
│  │  Back to login            │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Fields:**

| Field | Rules |
|---|---|
| `identifier` | `required, type: 'email'` |
| (CaptchaWidget) | **Mandatory** — GeeTest v4 challenge, backend fail-closed |

**Send Code Flow (GeeTest mandatory — backend fail-closed):**

1. User enters email (trimmed + normalized client-side)
2. "Send Code" button is disabled until email passes client-side validation
3. User clicks "Send Code" → `CaptchaWidget` loads GeeTest v4 SDK
4. GeeTest challenge popup appears → user completes verification
5. `CaptchaWidget.onCaptchaReady` fires with `{ captchaOutput, lotNumber, passToken, genTime }`
6. These 4 params are merged into `POST /auth/code/send` with `purpose: 'RESET_PASSWORD'`, `identityType: 'EMAIL'`
7. On success: `navigate('/verify-email', { state: { identifier, purpose: 'RESET_PASSWORD', identityType: 'EMAIL' } })`
8. On captcha failure (`GEE_TEST_FAILED 10010`): show error "安全验证失败，请重试", reset captcha, allow retry
9. On GeeTest SDK load failure: show error "安全验证服务暂不可用", disable send button

**Behavior:**
1. Email format validated before enabling "Send Code" button
2. Captcha must successfully complete before API call is made
3. On `USER_NOT_FOUND (40001)`: show generic "If this email is registered, a code has been sent." (don't reveal whether email exists — security best practice)

### 7.4 `EmailVerificationPage`

**File:** `src/customer/src/pages/EmailVerificationPage.tsx`

```
┌─────────────────────────────────┐
│         AuthCard                 │
│  ┌───────────────────────────┐  │
│  │    "Verify Your Email"    │  │
│  │                           │  │
│  │  Code sent to j***@e***.com│  │
│  │                           │  │
│  │  [VerificationCodeInput ] │  │
│  │                           │  │
│  │  [Verify (loading)      ] │  │
│  │                           │  │
│  │  Didn't receive code? Resend (30s) │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Behavior:**
1. Requires `location.state` with `{ identifier, purpose, identityType }`
2. Displays masked email: first char + `***` + `@` + first char + `***` + TLD
3. `VerificationCodeInput` auto-submits when 6 digits entered (configurable)
4. **Verify:**
   - Call `POST /auth/code/verify`
   - On success and `purpose === 'REGISTER'`: `navigate('/login')` with success message
   - On success and `purpose === 'RESET_PASSWORD'`: `navigate('/reset-password', { state: { identifier, code } })`
   - On `CODE_INVALID (40050)`: shake animation + error message, clear input, allow retry (max 3 attempts)
   - On `CODE_EXPIRED (40051)`: auto-trigger resend flow
5. **Resend (GeeTest required — same as initial send):**
   - User clicks "Resend Code" → `CaptchaWidget` presents GeeTest challenge
   - On captcha success → calls `POST /auth/code/send` with original `identifier`, `identityType`, `purpose`
   - 60s cooldown starts on success
   - Dev mode: bypass params used, no UI popup
6. Maximum 3 verify attempts before requiring a new code send

### 7.5 `PasswordResetPage`

**File:** `src/customer/src/pages/PasswordResetPage.tsx`

**Fields:**

| Field | Rules |
|---|---|
| `newPassword` | `required, minLength: 8, maxLength: 128, custom: passwordComplexity` |
| `confirmPassword` | `required, must match newPassword` |
| (PasswordStrengthBar) | Visual feedback widget |

**Behavior:**
1. Requires `location.state` with `{ identifier, code }`
2. Submit: call `POST /auth/password/reset`
3. On success: `navigate('/login')` with success message
4. On ``PASSWORD_REUSED (40060)`: show "Cannot reuse a previous password."
5. On `PASSWORD_SAME (40061)`: show "New password must differ from current password."

### 7.6 `MfaVerifyPage`

**File:** `src/customer/src/pages/MfaVerifyPage.tsx`

```
┌─────────────────────────────────┐
│         AuthCard                 │
│  ┌───────────────────────────┐  │
│  │  Back to login            │  │
│  │                           │  │
│  │  "Two-Factor Auth" title │  │
│  │  "Enter code from your   │  │
│  │   authenticator app"     │  │
│  │                           │  │
│  │  [VerificationCodeInput ] │  │
│  │                           │  │
│  │  [Verify (loading)      ] │  │
│  │                           │  │
│  │  Use backup code instead │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Behavior:**
1. Requires `location.state` with `{ mfaTypes: string[], loginIdentifier: string, credential: string, captchaParams: CaptchaParams }`
2. Uses **re-login with MFA code** approach: calls `POST /auth/login` with the stored `identifier`, `credential`, `mfaCode`, and captcha params
3. On success: dispatch `MFA_VERIFY_SUCCESS`, `navigate('/')`
4. On failure: show error, clear code input, allow retry
5. **"Use backup code":** toggle switches input to 8-character alphanumeric field
6. **"Back to login":** clears MFA context (clears credential from memory), `navigate('/login')`
7. Auto-submit when 6 digits entered (for TOTP) or manual submit for backup codes
8. `useEffect` cleanup: on unmount, dispatch `CLEAR_MFA_CONTEXT` to clear credential from memory

**Critical security:**
- `credential` (password) is ONLY in `mfaContext` of React state, NEVER written to localStorage
- On component unmount (user navigates away), credential is garbage-collected from memory
- If `location.state` is missing required keys, redirect to `/login`
- No back-button vulnerability: `location.state` is cleared when user navigates back

### 7.7 Shared Components

#### `AuthCard` — `components/AuthCard.tsx`
```tsx
interface AuthCardProps {
  title?: React.ReactNode
  subtitle?: React.ReactNode
  children: React.ReactNode
  maxWidth?: number                     // default 400
}
```
- Centered card with `boxShadow`, `borderRadius: 8`, max-width (default 400px)
- Responsive: full-width on `< 480px` with 16px margin
- Ant Design `<Card>` with theme tokens
- Renders `<LanguageSwitcher>` in top-right corner
- Applies `aria-label="Authentication"` and `role="main"`

#### `PasswordStrengthBar` — `components/PasswordStrengthBar.tsx`
```tsx
interface PasswordStrengthBarProps {
  password: string
  username?: string                     // for "no username substring" check
  email?: string                        // for "no email prefix" check
}
```
- Evaluates password against all 4 backend rules via `passwordValidator.ts`
- Visual states: 4-segment progress bar
  - 0-1 rules passed: red (`#ff4d4f`), label "Weak"
  - 2 rules: orange (`#faad14`), label "Moderate"
  - 3 rules: blue (`#1677ff`), label "Good"
  - 4 rules: green (`#52c41a`), label "Strong"
- Animated transitions between states
- Uses `role="progressbar"`, `aria-valuenow`, `aria-valuemin=0`, `aria-valuemax=4`
- Updates in real-time as user types (debounced 100ms)

#### `CaptchaWidget` — `components/CaptchaWidget.tsx`
```tsx
interface CaptchaWidgetProps {
  onCaptchaReady: (params: CaptchaParams) => void
  onCaptchaError?: (error: Error) => void
  onCaptchaReset?: () => void
  failMode: 'open' | 'closed'          // login=fail-open, code-send=fail-closed
}
```
- Loads GeeTest v4 SDK dynamically (`initGeetest4`)
- Dev mode (`VITE_SKIP_CAPTCHA=true`): immediately fires `onCaptchaReady` with bypass params
- Prod mode: renders captcha container, handles success/error/close callbacks
- Auto-reload on failure (fail-open mode skips on repeated failure)
- Exposes `reset()` method via `useImperativeHandle` for parent to trigger reset
- Displays loading skeleton while SDK loads

#### `VerificationCodeInput` — `components/VerificationCodeInput.tsx`
```tsx
interface VerificationCodeInputProps {
  value: string
  onChange: (code: string) => void
  length?: number                       // default 6
  onComplete?: (code: string) => void   // fired when all digits entered
  disabled?: boolean
  error?: boolean
  sendCodeButton?: React.ReactNode      // injected CountdownButton
}
```
- 6 separate `<Input>` segments, each accepting 1 digit (0-9)
- Auto-focus advances to next segment on digit entry
- Backspace in empty segment moves focus to previous
- Paste support: pasting 6 digits fills all segments
- Keyboard-only accessible: ArrowLeft/ArrowRight to move between segments
- `onComplete` fires when all segments filled
- Visual error state: red border + shake animation on invalid code
- Uses `aria-label="Verification code, digit {n} of {length}"` for each segment

#### `CountdownButton` — `components/CountdownButton.tsx`
```tsx
interface CountdownButtonProps {
  onClick: () => Promise<void> | void
  cooldownSeconds?: number              // default 60
  children: React.ReactNode             // label when idle
  loading?: boolean
  disabled?: boolean
}
```
- Uses `useCountdown` hook
- States:
  - `idle`: normal button, full label
  - `loading`: spinner + "Sending..."
  - `countdown`: disabled, shows "Resend in Xs"
  - `complete`: returns to idle

#### `LoadingOverlay` — `components/LoadingOverlay.tsx`
- Full-viewport centered `<Spin>` with semi-transparent backdrop
- Uses `role="status"`, `aria-live="polite"`, `aria-label="Loading"`
- Respects `prefers-reduced-motion` media query (disables spin animation)

#### `ErrorAlert` — `components/ErrorAlert.tsx`
```tsx
interface ErrorAlertProps {
  message: string
  type?: 'error' | 'warning' | 'info'
  closable?: boolean                    // default true
  onClose?: () => void
  retryAction?: () => void              // optional retry button
}
```
- Ant Design `<Alert>` with `role="alert"` for screen reader announcement
- Auto-dismisses after 8 seconds (unless user hovers)
- Optional retry button for recoverable errors

## 8. Custom Hooks

### `useCountdown(seconds: number)`
```ts
Return: {
  count: number           // current remaining seconds
  isRunning: boolean      // countdown active?
  start: () => void       // begin countdown
  reset: () => void       // reset to initial value
}
```
- Uses `setInterval` (1s interval), cleaned up on unmount
- Counts from `seconds` to 0, then auto-stops
- `start()` is a no-op if already running

### `usePasswordStrength(password, username?, email?)`
```ts
Return: {
  score: 0 | 1 | 2 | 3 | 4    // number of rules passed
  checks: {
    length: boolean
    uppercase: boolean
    lowercase: boolean
    digit: boolean
    special: boolean
    noUsernameSubstring: boolean
    noEmailSubstring: boolean
  }
  label: string                // i18n label for current score
  color: string                // CSS color
}
```
- Recalculates on `password` change (debounced 100ms)
- All checks are pure functions from `passwordValidator.ts`

### `useFormError()`
```ts
Return: {
  error: string | null
  fieldErrors: Record<string, string>
  setError: (msg: string) => void
  setFieldError: (field: string, msg: string) => void
  clearErrors: () => void
  parseApiError: (error: unknown) => void   // extracts from AxiosError
}
```
- Centralizes error state for a page form
- `parseApiError` maps backend error codes to field-specific messages

### `useCaptcha(failMode: 'open' | 'closed')`
```ts
Return: {
  captchaParams: CaptchaParams | null
  isReady: boolean
  isLoading: boolean
  error: string | null
  reset: () => void
}
```
- Manages GeeTest lifecycle
- In fail-open mode: ignores captcha errors, returns bypass params
- In fail-closed mode: surfaces errors to caller

### `useVerificationCode(identifier, identityType, purpose)`
```ts
Return: {
  sendCode: () => Promise<void>
  verifyCode: (code: string) => Promise<boolean>
  isSending: boolean
  isVerifying: boolean
  cooldown: number
  canResend: boolean
  attempts: number
}
```
- Orchestrates the send/verify code flow
- Tracks attempt count (max 3 before requiring new code send)
- Wraps API calls with error handling

## 9. Utilities

### `utils/tokenManager.ts`
```ts
// Single source of truth for token storage
// All other modules MUST use this, never access localStorage directly
export const tokenManager = {
  getAccessToken(): string | null
  setAccessToken(token: string): void
  getRefreshToken(): string | null
  setRefreshToken(token: string): void
  getExpiresAt(): number | null
  setExpiresAt(expiresAt: number): void
  getRememberedIdentifier(): string | null
  setRememberedIdentifier(identifier: string): void
  clearRememberedIdentifier(): void
  clearAll(): void   // clears all auth-related keys
}
```
- Keys: `'token'`, `'refreshToken'`, `'expiresAt'`, `'rememberedIdentifier'`
- All methods are synchronous and wrapped in try-catch (localStorage can throw in private browsing)

### `utils/errorHandler.ts`
```ts
// Maps backend ErrorCode → i18n key
const ERROR_CODE_MESSAGE_MAP: Record<number, string> = {
  [ErrorCode.INVALID_CREDENTIALS]: 'auth.invalid_credentials',
  [ErrorCode.ACCOUNT_LOCKED]: 'auth.account_locked',
  [ErrorCode.ACCOUNT_DISABLED]: 'auth.account_disabled',
  [ErrorCode.ACCOUNT_PENDING]: 'auth.account_pending',
  [ErrorCode.MFA_INVALID]: 'auth.mfa_invalid',
  [ErrorCode.USERNAME_EXISTS]: 'auth.username_exists',
  [ErrorCode.EMAIL_EXISTS]: 'auth.email_exists',
  [ErrorCode.PHONE_EXISTS]: 'auth.phone_exists',
  [ErrorCode.VERIFICATION_CODE_INVALID]: 'auth.code_invalid',
  [ErrorCode.VERIFICATION_CODE_EXPIRED]: 'auth.code_expired',
  [ErrorCode.GEE_TEST_FAILED]: 'auth.captcha_failed',
  [ErrorCode.PASSWORD_REUSED]: 'auth.password_reused',
  [ErrorCode.PASSWORD_SAME]: 'auth.password_same',
  [ErrorCode.TOKEN_EXPIRED]: 'auth.token_expired',
  [ErrorCode.REFRESH_TOKEN_REPLAY]: 'auth.session_expired',
  [ErrorCode.SERVICE_UNAVAILABLE]: 'common.service_unavailable',
  [ErrorCode.INTERNAL_ERROR]: 'common.internal_error',
}

// Extracts a user-facing message from any error type
export function extractErrorMessage(error: unknown): {
  message: string
  code: ErrorCodeValue | null
  field?: string
  retryAfterSeconds?: number
}

// Determines which form field an error belongs to
export function getErrorField(code: ErrorCodeValue): string | null

// Type guard for Axios errors
export function isAxiosError(error: unknown): error is AxiosError<ApiError>
```

### `utils/passwordValidator.ts`
```ts
export interface PasswordCheckResult {
  length: boolean          // 8-128 chars
  hasUpper: boolean        // [A-Z]
  hasLower: boolean        // [a-z]
  hasDigit: boolean        // [0-9]
  hasSpecial: boolean      // [@$!%*#?&]
  noUsernameMatch: boolean // no 3-char substring from username
  noEmailMatch: boolean    // no 3-char substring from email local part
}

export function evaluatePassword(
  password: string,
  username?: string,
  email?: string,
): PasswordCheckResult

// Returns count of passing checks (0-4: length + 3-of-4 character classes)
export function calculatePasswordScore(result: PasswordCheckResult): number

// Checks username substring (3+ consecutive chars, case-insensitive)
export function checkUsernameSubstring(password: string, username: string): boolean

// Checks email local part substring (3+ consecutive chars, case-insensitive)
export function checkEmailSubstring(password: string, email: string): boolean
```

### `utils/constants.ts`
```ts
export const TIMEOUTS = {
  API_REQUEST: 10000,          // 10s
  CAPTCHA_LOAD: 15000,        // 15s
  TOKEN_REFRESH: 5000,        // 5s
  CODE_RESEND_COOLDOWN: 60,   // s
  ERROR_AUTO_DISMISS: 8000,   // 8s
  SESSION_IDLE_WARNING: 840,  // 14min (1min before 15min token expiry)
} as const

export const LIMITS = {
  PASSWORD_MIN: 8,
  PASSWORD_MAX: 128,
  USERNAME_MIN: 3,
  USERNAME_MAX: 64,
  MFA_CODE_LENGTH: 6,
  BACKUP_CODE_LENGTH: 8,
  MAX_CODE_ATTEMPTS: 3,
  MAX_LOGIN_ATTEMPTS: 5,       // matches backend lockout threshold
} as const

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  EXPIRES_AT: 'expiresAt',
  REMEMBERED_IDENTIFIER: 'rememberedIdentifier',
} as const
```

### `utils/sanitize.ts`
```ts
// Trims whitespace from user input strings
export function trimInput(value: string): string

// Normalizes email: trim + lowercase
export function normalizeEmail(email: string): string

// Masks email for display: "j***@e***.com"
export function maskEmail(email: string): string

// Strips HTML tags from string (defense-in-depth, React handles this normally)
export function stripHtml(input: string): string
```

## 10. Internationalization (i18n)

### Design Principles

- **Zero library dependency**: Pure JS/JSON implementation, no `i18next` or `react-intl` weight
- **Namespace-based**: Messages split by domain (`common`, `auth`, `validation`) so each namespace can be worked on independently
- **Extensible by adding files only**: Adding a new language requires only 3 JSON files in a new directory — zero TypeScript changes
- **Compile-time safety**: A TypeScript type (`MessageKeys`) is generated from the JSON keys so `t('auth.typo')` fails at build time
- **Runtime fallback chain**: key (lang) → key (default `zh-CN`) → key name (dev) / `''` (prod)
- **Ant Design integration**: `ConfigProvider` locale matched to i18n locale
- **Persistence**: User's language preference stored in `localStorage`, default detected from `navigator.language`

### Architecture: Adding a new language

To add **Japanese (ja)**, only 4 steps are needed — no code changes:

```
1. Create directory:     i18n/locales/messages/ja/
2. Copy zh-CN JSONs as template:
     cp zh-CN/common.json ja/common.json
     cp zh-CN/auth.json ja/auth.json
     cp zh-CN/validation.json ja/validation.json
3. Translate all string values in the 3 JSON files
4. Register in i18n/locales/ja.ts:
     import common from './messages/ja/common.json'
     import auth from './messages/ja/auth.json'
     import validation from './messages/ja/validation.json'
     export default { common, auth, validation }
5. Add to SUPPORTED_LOCALES in i18n/index.ts:
     { code: 'ja', label: '日本語', antdLocale: () => import('antd/locale/ja_JP') }
```

The `useTranslation` hook and all components automatically work with the new language — no component code changes.

### Namespace Key Convention

```
namespace.key_name

Namespaces:  common | auth | validation
Key format:  snake_case, dot-separated
Examples:    auth.login_title, validation.password_min_length, common.loading
Params:      {paramName} for interpolation — t('auth.code_sent', { email: 'j***@e***.com' })
```

### Configuration

```ts
// src/customer/src/i18n/config.ts

export interface LocaleConfig {
  code: string                    // ISO 639-1 + region: 'zh-CN', 'en', 'ja'
  label: string                   // Native name: '简体中文', 'English', '日本語'
  antdLocale: () => Promise<any>  // Dynamic import of antd locale
}

export const SUPPORTED_LOCALES: LocaleConfig[] = [
  {
    code: 'zh-CN',
    label: '简体中文',
    antdLocale: () => import('antd/locale/zh_CN'),
  },
  {
    code: 'en',
    label: 'English',
    antdLocale: () => import('antd/locale/en_US'),
  },
  // Adding Japanese in the future:
  // {
  //   code: 'ja',
  //   label: '日本語',
  //   antdLocale: () => import('antd/locale/ja_JP'),
  // },
]

export const DEFAULT_LOCALE = 'zh-CN'
export const FALLBACK_LOCALE = 'zh-CN'
```

### Locale Detection Priority

```
1. User's saved preference (localStorage key: 'locale')
2. Browser language (navigator.language) — matched against SUPPORTED_LOCALES
3. DEFAULT_LOCALE ('zh-CN')
```

### `useTranslation` Hook

```ts
// src/customer/src/i18n/useTranslation.ts

interface TranslationContextValue {
  locale: string                   // current locale code
  setLocale: (code: string) => Promise<void>  // switch locale + persist
  t: (key: string, params?: Record<string, string | number>) => string
  formatDate: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string
  formatNumber: (value: number, options?: Intl.NumberFormatOptions) => string
}

function useTranslation(): TranslationContextValue

// Usage in components:
const { t, locale, setLocale } = useTranslation()

// Simple key
t('auth.login_title')                          // "登录" (zh) / "Log In" (en)

// With parameters
t('auth.code_sent', { email: 'j***@e***.com' })
// → "验证码已发送至 j***@e***.com" (zh)
// → "Code sent to j***@e***.com" (en)

// With count
t('auth.error_rate_limited', { seconds: 30 })
// → "操作过于频繁，请等待 30 秒后再试" (zh)
// → "Too many attempts. Please wait 30 seconds." (en)

// Force specific locale (rare, for tests/fallback)
t('auth.login_title', {}, 'en')               // "Log In"
```

### `LocaleProvider` Component

```tsx
// src/customer/src/i18n/LocaleProvider.tsx
// Wraps the app, provides TranslationContext + Ant Design ConfigProvider

function LocaleProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<string>(detectLocale)
  const [messages, setMessages] = useState<Messages>(loadMessages(locale))
  const [antdLocale, setAntdLocale] = useState<any>(null)

  // Load messages + antd locale in parallel on locale change
  const setLocale = useCallback(async (code: string) => {
    const msgs = await loadMessagesAsync(code)      // dynamic import
    const antd = await loadAntdLocaleAsync(code)     // dynamic import
    setMessages(msgs)
    setAntdLocale(antd)
    setLocaleState(code)
    localStorage.setItem('locale', code)
  }, [])

  const t = useCallback((key: string, params?: Record<string, string | number>) => {
    const [ns, ...rest] = key.split('.')
    const msgKey = rest.join('.')
    const value = messages[ns]?.[msgKey] ?? FALLBACK_MESSAGES[ns]?.[msgKey] ?? (import.meta.env.DEV ? key : '')
    return interpolate(value, params)
  }, [messages])

  return (
    <ConfigProvider locale={antdLocale?.default}>
      <TranslationContext.Provider value={{ locale, setLocale, t, formatDate, formatNumber }}>
        {children}
      </TranslationContext.Provider>
    </ConfigProvider>
  )
}
```

### Directory Structure

```
src/customer/src/i18n/
  index.ts                    # Barrel: re-exports LocaleProvider, useTranslation
  config.ts                   # SUPPORTED_LOCALES, DEFAULT_LOCALE
  useTranslation.ts           # Hook implementation + TranslationContext
  LocaleProvider.tsx           # Provider component, locale detection, message loading
  interpolate.ts              # {param} string interpolation utility
  types.ts                    # Messages type, TranslationContext type
  locales/
    zh-CN.ts                  # import + export default { common, auth, validation }
    en.ts                     # import + export default { common, auth, validation }
    messages/
      zh-CN/
        common.json           # App shell strings
        auth.json             # Auth flow strings (~45 keys)
        validation.json       # Form validation strings (~12 keys)
      en/
        common.json
        auth.json
        validation.json
```

### Message Files

**`messages/zh-CN/common.json`:**
```json
{
  "app_name": "账户中心",
  "loading": "加载中...",
  "submit": "提交",
  "cancel": "取消",
  "retry": "重试",
  "back": "返回",
  "close": "关闭",
  "service_unavailable": "服务暂不可用，请稍后重试",
  "internal_error": "系统内部错误，请稍后重试",
  "offline": "当前处于离线状态，部分功能可能不可用",
  "language": "语言",
  "switch_to": "切换至 {language}"
}
```

**`messages/en/common.json`:**
```json
{
  "app_name": "Account Center",
  "loading": "Loading...",
  "submit": "Submit",
  "cancel": "Cancel",
  "retry": "Retry",
  "back": "Back",
  "close": "Close",
  "service_unavailable": "Service temporarily unavailable. Please try again later.",
  "internal_error": "Internal server error. Please try again later.",
  "offline": "You are offline. Some features may be unavailable.",
  "language": "Language",
  "switch_to": "Switch to {language}"
}
```

**`messages/zh-CN/validation.json`:**
```json
{
  "required": "此项为必填项",
  "email_invalid": "请输入有效的邮箱地址",
  "username_format": "用户名只能包含字母、数字、下划线和连字符",
  "username_min_length": "用户名长度不能少于3个字符",
  "username_max_length": "用户名长度不能超过64个字符",
  "password_min_length": "密码长度不能少于8个字符",
  "password_max_length": "密码长度不能超过128个字符",
  "password_complexity": "密码必须包含大写字母、小写字母、数字、特殊字符(@$!%*#?&)中的至少三类",
  "password_username": "密码不能包含连续3个及以上与用户名相同的字符",
  "password_email": "密码不能包含连续3个及以上与邮箱前缀相同的字符",
  "password_mismatch": "两次输入的密码不一致",
  "code_length": "验证码为6位数字",
  "accept_terms": "请阅读并同意服务条款",
  "captcha_required": "请完成安全验证"
}
```

**`messages/en/validation.json`:**
```json
{
  "required": "This field is required",
  "email_invalid": "Please enter a valid email address",
  "username_format": "Username can only contain letters, numbers, underscores, and hyphens",
  "username_min_length": "Username must be at least 3 characters",
  "username_max_length": "Username must not exceed 64 characters",
  "password_min_length": "Password must be at least 8 characters",
  "password_max_length": "Password must not exceed 128 characters",
  "password_complexity": "Password must contain at least 3 of: uppercase, lowercase, digits, special characters (@$!%*#?&)",
  "password_username": "Password must not contain 3 or more consecutive characters from your username",
  "password_email": "Password must not contain 3 or more consecutive characters from your email prefix",
  "password_mismatch": "Passwords do not match",
  "code_length": "Verification code must be 6 digits",
  "accept_terms": "Please read and agree to the Terms of Service",
  "captcha_required": "Please complete the security verification"
}
```

**`messages/zh-CN/auth.json`:**
```json
{
  "login_title": "欢迎回来",
  "login_subtitle": "登录您的账户",
  "login_button": "登录",
  "register_title": "创建账户",
  "register_subtitle": "填写信息以创建新账户",
  "register_button": "创建账户",
  "login_link": "已有账户？立即登录",
  "register_link": "没有账户？立即注册",
  "forgot_password": "忘记密码？",
  "forgot_password_title": "找回密码",
  "forgot_password_subtitle": "输入注册邮箱，我们将发送验证码",
  "send_code": "发送验证码",
  "sending": "发送中...",
  "resend_code": "重新发送",
  "resend_in": "{seconds}秒后重新发送",
  "verify_email_title": "验证邮箱",
  "code_sent": "验证码已发送至 {email}",
  "no_code_received": "没有收到验证码？",
  "reset_password_title": "设置新密码",
  "reset_password_subtitle": "请输入您的新密码",
  "reset_password_button": "重置密码",
  "mfa_title": "两步验证",
  "mfa_subtitle": "请输入您的验证器应用中的6位数字验证码",
  "mfa_backup_code": "使用备用验证码",
  "mfa_totp_code": "使用验证器应用",
  "mfa_verify": "验证",
  "mfa_back_to_login": "返回登录",
  "registration_success": "注册成功，请登录",
  "password_reset_success": "密码重置成功，请使用新密码登录",
  "logout_success": "已退出登录",
  "remember_me": "记住我",
  "accept_terms_label": "我已阅读并同意",
  "terms_of_service": "服务条款",
  "privacy_policy": "隐私政策",
  "and": "和",
  "username_label": "用户名",
  "username_placeholder": "请输入用户名",
  "email_label": "邮箱",
  "email_placeholder": "请输入邮箱地址",
  "phone_label": "手机号（选填）",
  "phone_placeholder": "请输入手机号",
  "password_label": "密码",
  "password_placeholder": "请输入密码",
  "confirm_password_label": "确认密码",
  "confirm_password_placeholder": "请再次输入密码",
  "mfa_code_label": "MFA 验证码",
  "mfa_code_placeholder": "6位验证码",
  "identifier_placeholder": "用户名 / 邮箱 / 手机号",
  "password_strength_weak": "弱",
  "password_strength_moderate": "一般",
  "password_strength_good": "良好",
  "password_strength_strong": "强",
  
  "error_invalid_credentials": "用户名或密码错误",
  "error_account_locked": "账户已被锁定，请于 {time} 后重试",
  "error_account_disabled": "账户已被禁用，请联系客服",
  "error_account_pending": "账户尚未激活，请先验证邮箱",
  "error_mfa_invalid": "验证码无效，请重试",
  "error_username_exists": "用户名已被使用",
  "error_email_exists": "邮箱已被注册",
  "error_phone_exists": "手机号已被注册",
  "error_code_invalid": "验证码无效",
  "error_code_expired": "验证码已过期，请重新获取",
  "error_captcha_failed": "安全验证失败，请重试",
  "error_captcha_unavailable": "安全验证服务暂不可用，请稍后重试",
  "error_password_reused": "不能使用之前使用过的密码",
  "error_password_same": "新密码不能与当前密码相同",
  "error_session_expired": "会话已过期，请重新登录",
  "error_network": "网络连接失败，请检查网络后重试",
  "error_timeout": "请求超时，请重试",
  "error_server": "服务器错误，请稍后重试",
  "error_rate_limited": "操作过于频繁，请等待 {seconds} 秒后再试",
  "error_unknown": "未知错误，请重试",
  "error_code_max_attempts": "验证码尝试次数过多，请重新获取"
}
```

**`messages/en/auth.json`:**
```json
{
  "login_title": "Welcome Back",
  "login_subtitle": "Log in to your account",
  "login_button": "Log In",
  "register_title": "Create Account",
  "register_subtitle": "Fill in your information to get started",
  "register_button": "Create Account",
  "login_link": "Already have an account? Log in",
  "register_link": "Don't have an account? Register",
  "forgot_password": "Forgot password?",
  "forgot_password_title": "Reset Password",
  "forgot_password_subtitle": "Enter your registered email to receive a verification code",
  "send_code": "Send Code",
  "sending": "Sending...",
  "resend_code": "Resend",
  "resend_in": "Resend in {seconds}s",
  "verify_email_title": "Verify Your Email",
  "code_sent": "Verification code sent to {email}",
  "no_code_received": "Didn't receive the code?",
  "reset_password_title": "Set New Password",
  "reset_password_subtitle": "Enter your new password",
  "reset_password_button": "Reset Password",
  "mfa_title": "Two-Factor Authentication",
  "mfa_subtitle": "Enter the 6-digit code from your authenticator app",
  "mfa_backup_code": "Use backup code instead",
  "mfa_totp_code": "Use authenticator app",
  "mfa_verify": "Verify",
  "mfa_back_to_login": "Back to login",
  "registration_success": "Registration successful. Please log in.",
  "password_reset_success": "Password reset successful. Please log in with your new password.",
  "logout_success": "Logged out successfully",
  "remember_me": "Remember me",
  "accept_terms_label": "I have read and agree to the",
  "terms_of_service": "Terms of Service",
  "privacy_policy": "Privacy Policy",
  "and": "and",
  "username_label": "Username",
  "username_placeholder": "Enter your username",
  "email_label": "Email",
  "email_placeholder": "Enter your email address",
  "phone_label": "Phone (optional)",
  "phone_placeholder": "Enter your phone number",
  "password_label": "Password",
  "password_placeholder": "Enter your password",
  "confirm_password_label": "Confirm Password",
  "confirm_password_placeholder": "Re-enter your password",
  "mfa_code_label": "MFA Code",
  "mfa_code_placeholder": "6-digit code",
  "identifier_placeholder": "Username / Email / Phone",
  "password_strength_weak": "Weak",
  "password_strength_moderate": "Moderate",
  "password_strength_good": "Good",
  "password_strength_strong": "Strong",
  
  "error_invalid_credentials": "Invalid username or password",
  "error_account_locked": "Account is locked. Try again in {time}.",
  "error_account_disabled": "Account has been disabled. Contact support.",
  "error_account_pending": "Account is not activated. Please verify your email.",
  "error_mfa_invalid": "Invalid MFA code. Please try again.",
  "error_username_exists": "Username is already taken",
  "error_email_exists": "Email is already registered",
  "error_phone_exists": "Phone number is already registered",
  "error_code_invalid": "Invalid verification code",
  "error_code_expired": "Verification code has expired. Request a new one.",
  "error_captcha_failed": "Security verification failed. Please try again.",
  "error_captcha_unavailable": "Security verification service is temporarily unavailable.",
  "error_password_reused": "Cannot reuse a previously used password",
  "error_password_same": "New password must differ from current password",
  "error_session_expired": "Session expired. Please log in again.",
  "error_network": "Network error. Check your connection and try again.",
  "error_timeout": "Request timed out. Please try again.",
  "error_server": "Server error. Please try again later.",
  "error_rate_limited": "Too many attempts. Please wait {seconds} seconds.",
  "error_unknown": "An unknown error occurred. Please try again.",
  "error_code_max_attempts": "Too many attempts. Please request a new code."
}
```

### `interpolate.ts` — Parameterized String Utility

```ts
// Replaces {paramName} placeholders with values
// interpolate('Code sent to {email}', { email: 'j***@e***.com' })
// → 'Code sent to j***@e***.com'
//
// Missing params are left as-is in dev, replaced with '' in prod
export function interpolate(
  template: string,
  params?: Record<string, string | number>,
): string
```

### Ant Design Locale Integration

When locale changes, Ant Design's internal text (date pickers, form validation messages) switches automatically via `ConfigProvider`:

```tsx
// Switching to English:
// - Ant Design buttons: "确定" → "OK", "取消" → "Cancel"
// - Form validation: "此项为必填项" → "This field is required" (fallback, our custom rules take priority)
// - Date format: YYYY-MM-DD → MM/DD/YYYY (if applicable)
```

Our custom form validation rules use `t('validation.required')` which takes priority over Ant Design's default messages, ensuring consistency with the selected locale.

## 11. Security Design

### 11.1 Defense in Depth

| Layer | Measure |
|---|---|
| **Transport** | HTTPS enforced in production (backend HSTS header) |
| **Content** | CSP via `<meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; connect-src 'self' https://*.geetest.com;">` |
| **Token storage** | localStorage with 15min access token TTL (same trade-off as admin, documented) |
| **XSS** | React JSX escaping, no `dangerouslySetInnerHTML`, `stripHtml()` on user-origin strings |
| **CSRF** | Not applicable (Bearer token, no cookies) |
| **Input** | All form inputs trimmed/normalized before API calls, `sanitize.ts` utilities |
| **Secrets** | Password/credential only in React state/memory, cleared on unmount, never logged |
| **URL safety** | No tokens or secrets in URL parameters, only in `location.state` (memory) |
| **Dependencies** | Same versions as admin (already vetted) |

### 11.2 MFA Credential Protection
```
┌─────────────────────────────────────────────┐
│ MFA flow — credential lifecycle             │
│                                              │
│ LoginPage                                    │
│   credential in form state (memory)          │
│   ↓ POST /auth/login (HTTPS encrypted)       │
│   ↓ mfaRequired=true returned               │
│   ↓ credential passed to /mfa-verify         │
│         via navigate(..., { state })         │
│                                              │
│ MfaVerifyPage                                │
│   credential from location.state (memory)    │
│   ↓ POST /auth/login again with mfaCode     │
│   ↓ success → dispatch LOGIN_SUCCESS        │
│   ↓ credential cleared from memory           │
│                                              │
│ useEffect cleanup on unmount:                │
│   → credential purged from React state       │
│   → Nothing in localStorage                  │
│   → Nothing in URL                           │
│   → Back button after success = guarded      │
└─────────────────────────────────────────────┘
```

### 11.3 Silent Token Refresh Security

- Only one refresh in-flight at a time (`isRefreshing` flag)
- Pending requests queued, not failed
- Refresh token is rotated server-side (Lua atomic rotation + family revocation)
- If refresh fails (expired, revoked, replay detected): full logout, clear all tokens
- Refresh not attempted for `/auth/login`, `/auth/register`, `/auth/code/*` endpoints

## 12. Accessibility (WCAG 2.1 AA)

| Requirement | Implementation |
|---|---|
| **Keyboard navigation** | All form controls focusable and operable via keyboard. Tab order matches visual order. `VerificationCodeInput` segments navigable with ArrowLeft/ArrowRight. |
| **Focus indicators** | Visible focus ring on all interactive elements (Ant Design default + custom `:focus-visible` enhancement) |
| **ARIA roles** | `role="main"` on AuthCard, `role="alert"` on ErrorAlert, `role="progressbar"` on PasswordStrengthBar, `role="status"` on LoadingOverlay |
| **ARIA labels** | `aria-label` on icon-only buttons (LanguageSwitcher, password toggle), segmented code inputs |
| **Screen reader** | `aria-live="polite"` for loading state announcements, `aria-live="assertive"` for error messages |
| **Color contrast** | All text meets 4.5:1 minimum contrast ratio (Ant Design 5 design tokens ensure this) |
| **Reduced motion** | Respects `prefers-reduced-motion` — disables shake animations, spin animation, transitions |
| **Zoom support** | Layout works at 200% browser zoom without horizontal scroll |
| **Error identification** | Errors identified by both color AND icon + text — not color-alone |
| **Touch targets** | All interactive elements minimum 44x44px (Ant Design `size="large"` achieves this) |

## 13. Performance Strategy

| Measure | Detail |
|---|---|
| **Code splitting** | All 6 page components loaded via `React.lazy(() => import(...))`, separate chunks |
| **Ant Design tree-shaking** | Vite + antd v5 with CSS-in-JS, only used components bundled |
| **Suspense boundaries** | Per-route Suspense with skeleton fallback, not single app-level spinner |
| **Memoization** | `React.memo` on heavy components (PasswordStrengthBar, VerificationCodeInput) |
| **useCallback/useMemo** | Stability for context values, API functions, callback props |
| **Debounced validation** | Password strength calculation debounced 100ms |
| **i18n bundle** | JSON locale files loaded statically (not dynamic) — tree-shaken in production |
| **GeeTest SDK** | Loaded asynchronously, non-blocking. `<script async>` injection |
| **Build target** | ES2020+ with `nomodule` fallback consideration |
| **Image optimization** | SVG for logo (small, scalable) |

## 14. Testing Strategy

### Unit Tests (Vitest) — `src/customer/src/__tests__/`

| File | Tests |
|---|---|
| `utils/passwordValidator.test.ts` | All 7 rules: length, upper, lower, digit, special, username check, email check. Edge: empty, Unicode, max length |
| `utils/errorHandler.test.ts` | Each error code → correct i18n key. Network error, timeout, unknown error |
| `utils/tokenManager.test.ts` | set/get/clear, localStorage unavailable simulation, clearAll |
| `utils/sanitize.test.ts` | trimInput, normalizeEmail, maskEmail, stripHtml |
| `hooks/useCountdown.test.ts` | start, completion, reset, unmount cleanup |
| `hooks/usePasswordStrength.test.ts` | Score calculation, debounce, username/email check |

### Component Tests (Vitest + @testing-library/react)

| File | Tests |
|---|---|
| `PasswordStrengthBar.test.tsx` | Renders 4 segments, correct color per score, aria attributes |
| `VerificationCodeInput.test.tsx` | Digit entry, auto-advance, backspace, paste, onComplete, maxDigits |
| `CaptchaWidget.test.tsx` | Dev mode bypass, loading state, error state |
| `CountdownButton.test.tsx` | Click → countdown → completion, reset |
| `ErrorAlert.test.tsx` | Displays message, dismissable, retry button, auto-dismiss |

### Page Tests (Vitest + @testing-library/react)

| File | Tests |
|---|---|
| `LoginPage.test.tsx` | Renders form, validations fire, submit calls API, MFA redirect, error display, rate limit message |
| `RegisterPage.test.tsx` | All validations, password strength bar updates, code send, submit |

### Integration Tests

| File | Tests |
|---|---|
| `auth-flow.test.tsx` | Register → Login → MFA → Token refresh → Logout — full happy path |

### E2E Tests (Playwright — future phase)

- Full register → verify → login → MFA → logout flow
- Forgot password → reset → login flow
- Rate limiting UX
- Mobile viewport testing
- Keyboard navigation testing
- Screen reader testing

## 15. Implementation Phases (6 Batches)

### Batch 1 — Project Scaffold + Type System + Utilities

**Goal:** Vite project initializes, TypeScript compiles, all pure utilities are tested.

1. Create `src/customer/` with `package.json`, `vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`, `index.html`
2. `.env.development` with `VITE_SKIP_CAPTCHA=true`, `VITE_API_BASE_URL=/api/v1`
3. `.eslintrc.cjs`, `.prettierrc`, `vitest.config.ts`
4. `src/types/api.ts` — `R<T>`, `FieldError`, `IPageData<T>`
5. `src/types/auth.ts` — All auth DTOs, response types
6. `src/types/errors.ts` — `ErrorCode` constant object, `HttpStatus`, `ApiError`
7. `src/types/user.ts` — `UserStatus`, `IdentityType`, `MfaType` enums
8. `src/types/index.ts` — Re-export barrel
9. `src/utils/constants.ts` — `TIMEOUTS`, `LIMITS`, `STORAGE_KEYS`
10. `src/utils/sanitize.ts` — `trimInput`, `normalizeEmail`, `maskEmail`, `stripHtml`
11. `src/utils/passwordValidator.ts` — All 7 check functions + `calculatePasswordScore`
12. `src/utils/errorHandler.ts` — `ERROR_CODE_MESSAGE_MAP`, `extractErrorMessage`, `getErrorField`
13. `src/utils/tokenManager.ts` — Encapsulated localStorage access
14. `src/utils/logger.ts` — Structured logger
15. `src/styles/global.css` — Design tokens, reset, font-family
16. `src/styles/responsive.css` — Breakpoint definitions
17. Write **unit tests** for all utilities
18. Verify: `npm run dev` starts, `npm run build` succeeds, `npm test` passes

### Batch 2 — API Layer + AuthContext

1. `src/api/client.ts` — Axios instance with full interceptor chain (request + 7-layer response error handling + silent refresh queue)
2. `src/api/auth.ts` — All auth endpoint functions with JSDoc
3. `src/api/mfa.ts` — MFA verify function
4. `src/context/AuthReducer.ts` — `authReducer` with all action types
5. `src/context/authActions.ts` — Action creator functions
6. `src/context/AuthContext.tsx` — `AuthProvider` + `useAuth` hook with full state machine
7. `src/main.tsx` — Provider bootstrap (StrictMode → BrowserRouter → AuthProvider → App)
8. `src/App.tsx` — `AppRoutes` with `RequireNavigationState` guard
9. `src/components/SuspenseFallback.tsx`
10. `src/components/LoadingOverlay.tsx`
11. `src/components/AuthCard.tsx`
12. Verify: app boots, redirects unknown route to `/login`, auth state machine transitions work

### Batch 3 — i18n Infrastructure + Shared Components

1. `src/i18n/locales/zh-CN.ts` + all 3 JSON message files
2. `src/i18n/locales/en.ts` + all 3 JSON message files
3. `src/i18n/index.ts` — `useTranslation` hook, `LocaleProvider`
4. `src/components/LanguageSwitcher.tsx`
5. `src/components/ErrorAlert.tsx`
6. `src/components/CountdownButton.tsx`
7. `src/hooks/useCountdown.ts` — with tests
8. `src/hooks/usePasswordStrength.ts` — with tests
9. `src/hooks/useFormError.ts`
10. `src/components/PasswordStrengthBar.tsx` — with tests
11. `src/components/VerificationCodeInput.tsx` — with tests
12. `src/components/CaptchaWidget.tsx` — with tests
13. `src/components/PasswordInput.tsx`
14. `src/components/FormField.tsx`
15. `src/components/NoScriptFallback.tsx`
16. Verify: all component tests pass, i18n switching works

### Batch 4 — Login + Register Pages

1. `src/hooks/useCaptcha.ts`
2. `src/hooks/useVerificationCode.ts`
3. `src/pages/LoginPage.tsx` — full implementation
4. `src/pages/RegisterPage.tsx` — full implementation
5. Write page tests for both
6. Verify: can login (dev captcha bypass), can register with verification code, proper error handling

### Batch 5 — Password Reset + MFA Flow

1. `src/pages/ForgotPasswordPage.tsx`
2. `src/pages/EmailVerificationPage.tsx`
3. `src/pages/PasswordResetPage.tsx`
4. `src/pages/MfaVerifyPage.tsx`
5. `src/hooks/useMfaFlow.ts`
6. Write page tests
7. Verify: full forgot-password flow, full MFA flow

### Batch 6 — Polish + Hardening

1. Responsive testing (375px, 768px, 1024px, 1440px)
2. Accessibility audit (keyboard nav, screen reader, contrast)
3. Offline handling: detect `navigator.onLine` + `online`/`offline` events, show banner
4. Session idle warning: setInterval checks token expiry, warns at 14min
5. Cross-tab synchronization: `storage` event listener for logout propagation
6. Loading skeletons for all lazy-loaded pages
7. `.env.production` with `VITE_SKIP_CAPTCHA=false`
8. Production build optimization: `vite build` with rollup analyzer
9. Write integration test: full auth flow
10. Final code review: ensure no `localStorage` direct access outside `tokenManager`, no credential leaks, all i18n keys used

## 16. Verification Checklist

### Build & Type Safety
- [ ] `npm run build` succeeds with zero TS errors
- [ ] `npm run lint` passes with zero warnings
- [ ] `npm test` passes all unit + component + integration tests
- [ ] Bundle size: each page chunk < 50KB gzipped (excluding antd vendor)

### Functional
- [ ] Register: fill form → get code → verify → success → redirect to /login
- [ ] Register with existing username/email → field-specific error
- [ ] Login: identifier + password + captcha(dev bypass) → receive tokens → redirect to /
- [ ] Login invalid credentials → error message, form remains filled
- [ ] Login account locked → shows lockout time
- [ ] Login with MFA-enabled account → redirect to /mfa-verify
- [ ] MFA verify: enter TOTP → success → redirect to /
- [ ] MFA verify: enter invalid code → error, retry allowed
- [ ] MFA verify: leave page → credential cleared from memory
- [ ] Forgot password: email → code send → verify → set new password → login with new password
- [ ] Token refresh: access token expires → 401 → auto-refresh → retry succeeds
- [ ] Token refresh fails → full logout → redirected to /login
- [ ] Logout: clears tokens, redirects to /login

### Error Handling
- [ ] Network offline: shows offline banner, forms disabled
- [ ] Backend down: shows server error, retry button
- [ ] Rate limit (429): shows countdown message
- [ ] Validation error (422): field-specific error messages
- [ ] Timeout: shows timeout message, retry button
- [ ] Double-submit: button disabled while loading state is active

### Security
- [ ] No tokens in URL query parameters
- [ ] No credentials in localStorage (only tokens)
- [ ] Password never logged to console
- [ ] MFA credential cleared on component unmount
- [ ] Direct URL access to /verify-email without state → redirect to /login
- [ ] Direct URL access to /mfa-verify without state → redirect to /login
- [ ] Direct URL access to /reset-password without state → redirect to /login

### i18n
- [ ] Switch to English: all UI text changes
- [ ] Switch to Chinese: all UI text changes
- [ ] Ant Design components (date pickers, etc.) follow locale
- [ ] Error messages from backend mapped to correct locale

### Accessibility
- [ ] Tab order: identifier → password → MFA code(if shown) → login button → forgot password → register
- [ ] All form fields have visible focus indicators
- [ ] ErrorAlert announced by screen reader (`role="alert"`)
- [ ] PasswordStrengthBar has `role="progressbar"` with correct values
- [ ] VerificationCodeInput segments have `aria-label` with position
- [ ] 200% zoom: no horizontal scroll, all content visible
- [ ] `prefers-reduced-motion`: no animations

### Mobile Responsive
- [ ] 375px viewport: AuthCard full-width with 16px margin, fields stacked vertically
- [ ] 768px viewport: AuthCard centered with max-width
- [ ] Touch: all buttons have 44x44px minimum tap target
- [ ] Soft keyboard: form scrolls to focused field, not obscured

### Cross-Browser
- [ ] Chrome (latest 2 versions)
- [ ] Firefox (latest 2 versions)
- [ ] Safari (latest 2 versions)
- [ ] Edge (latest 2 versions)
