import { Suspense, lazy } from 'react'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import SuspenseFallback from './components/SuspenseFallback'

// Lazy-loaded auth pages
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'))
const EmailVerificationPage = lazy(() => import('./pages/EmailVerificationPage'))
const PasswordResetPage = lazy(() => import('./pages/PasswordResetPage'))
const MfaVerifyPage = lazy(() => import('./pages/MfaVerifyPage'))

// Lazy-loaded post-login pages
const UserLayout = lazy(() => import('./components/UserLayout'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Profile = lazy(() => import('./pages/Profile'))
const Security = lazy(() => import('./pages/Security'))

// Lazy-loaded legal pages
const TermsPage = lazy(() => import('./pages/TermsPage'))
const PrivacyPage = lazy(() => import('./pages/PrivacyPage'))

function RequireNavigationState({ requiredKeys, children }: { requiredKeys: string[]; children: React.ReactNode }) {
  const location = useLocation()
  if (!requiredKeys.every((key) => location.state && key in (location.state as Record<string, unknown>))) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { state } = useAuth()
  if (state.status !== 'authenticated') return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      {/* Post-login routes */}
      <Route
        element={
          <RequireAuth>
            <Suspense fallback={<SuspenseFallback />}>
              <UserLayout />
            </Suspense>
          </RequireAuth>
        }
      >
        <Route path="/dashboard" element={<Suspense fallback={<SuspenseFallback />}><Dashboard /></Suspense>} />
        <Route path="/profile" element={<Suspense fallback={<SuspenseFallback />}><Profile /></Suspense>} />
        <Route path="/security" element={<Suspense fallback={<SuspenseFallback />}><Security /></Suspense>} />
        <Route index element={<Navigate to="/dashboard" replace />} />
      </Route>

      {/* Auth routes */}
      <Route path="/login" element={<Suspense fallback={<SuspenseFallback />}><LoginPage /></Suspense>} />
      <Route path="/register" element={<Suspense fallback={<SuspenseFallback />}><RegisterPage /></Suspense>} />
      <Route path="/forgot-password" element={<Suspense fallback={<SuspenseFallback />}><ForgotPasswordPage /></Suspense>} />
      <Route path="/verify-email" element={
        <RequireNavigationState requiredKeys={['identifier', 'purpose']}>
          <Suspense fallback={<SuspenseFallback />}><EmailVerificationPage /></Suspense>
        </RequireNavigationState>
      } />
      <Route path="/mfa-verify" element={
        <RequireNavigationState requiredKeys={['mfaTypes', 'loginIdentifier', 'credential']}>
          <Suspense fallback={<SuspenseFallback />}><MfaVerifyPage /></Suspense>
        </RequireNavigationState>
      } />
      <Route path="/reset-password" element={
        <RequireNavigationState requiredKeys={['identifier', 'code']}>
          <Suspense fallback={<SuspenseFallback />}><PasswordResetPage /></Suspense>
        </RequireNavigationState>
      } />
      <Route path="/terms" element={<Suspense fallback={<SuspenseFallback />}><TermsPage /></Suspense>} />
      <Route path="/privacy" element={<Suspense fallback={<SuspenseFallback />}><PrivacyPage /></Suspense>} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}

export default App
