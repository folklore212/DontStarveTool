import { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'

interface AuthContextType {
  token: string | null
  isLoggedIn: boolean
  login: (token: string, refreshToken?: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('token'),
  )
  const navigate = useNavigate()
  const logoutInProgress = useRef(false)

  const login = useCallback((newToken: string, refreshToken?: string) => {
    localStorage.setItem('token', newToken)
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }
    setToken(newToken)
  }, [])

  const logout = useCallback(async () => {
    if (logoutInProgress.current) return
    logoutInProgress.current = true
    try {
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        await client.post('/auth/logout', refreshToken ? { refreshToken } : {})
      } catch {
        // ignore — the server-side logout is best-effort
      }
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      setToken(null)
      navigate('/login')
    } finally {
      logoutInProgress.current = false
    }
  }, [navigate])

  useEffect(() => {
    const handle401 = () => logout()
    window.addEventListener('auth:unauthorized', handle401)
    return () => window.removeEventListener('auth:unauthorized', handle401)
  }, [logout])

  useEffect(() => {
    const handleForbidden = () => navigate('/forbidden')
    window.addEventListener('auth:forbidden', handleForbidden)
    return () => window.removeEventListener('auth:forbidden', handleForbidden)
  }, [navigate])

  return (
    <AuthContext.Provider value={{ token, isLoggedIn: !!token, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
