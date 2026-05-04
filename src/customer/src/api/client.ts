import axios from 'axios'
import { tokenManager } from '../utils/tokenManager'
import { logger } from '../utils/logger'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const API_TIMEOUT = 10_000

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ---- Refresh queue ----
let isRefreshing = false
let pendingQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

function enqueueRequest(resolve: (token: string) => void, reject: (error: unknown) => void) {
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

// ---- Request interceptor ----
client.interceptors.request.use(
  (config) => {
    const token = tokenManager.getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const locale = tokenManager.getLocale() || 'zh-CN'
    config.headers['Accept-Language'] = locale
    return config
  },
  (error) => Promise.reject(error),
)

// ---- Response interceptor ----
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const url = error.config?.url || ''
    const method = error.config?.method?.toUpperCase() || ''

    // Network error (no response from server)
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        logger.warn('Request timed out', { url, method })
      } else {
        logger.error('Network error', { url, method, message: error.message })
      }
      return Promise.reject(error)
    }

    const status = error.response.status
    logger.debug('API response error', { url, method, status })

    // 401 — attempt silent token refresh
    if (status === 401) {
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register')
          || url.includes('/auth/code') || url.includes('/auth/password/reset')
          || url.includes('/auth/captcha-config')
      const isRefreshRequest = url.includes('/auth/refresh')
      if (isAuthEndpoint) return Promise.reject(error)

      if (!isRefreshRequest) {
        const refreshToken = tokenManager.getRefreshToken()

        if (refreshToken && !isRefreshing) {
          isRefreshing = true

          try {
            const response = await axios.post(
              `${API_BASE_URL}/auth/refresh`,
              { refreshToken },
              { headers: { 'Content-Type': 'application/json' }, timeout: 5_000 },
            )

            if (response.data?.code === 0) {
              const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data
              tokenManager.setAccessToken(accessToken)
              if (newRefreshToken) tokenManager.setRefreshToken(newRefreshToken)
              if (expiresIn) tokenManager.setExpiresAt(Date.now() + expiresIn * 1000)

              const retryConfig = { ...error.config }
              retryConfig.headers.Authorization = `Bearer ${accessToken}`

              onRefreshed(accessToken)
              isRefreshing = false
              return client(retryConfig)
            }
          } catch (refreshError) {
            logger.error('Token refresh failed', { error: String(refreshError) })
          }

          isRefreshing = false
          onRefreshFailed(error)
        } else if (isRefreshing) {
          // Another refresh is in progress — queue this request
          return new Promise((resolve, reject) => {
            enqueueRequest(
              (token: string) => {
                const retryConfig = { ...error.config }
                retryConfig.headers.Authorization = `Bearer ${token}`
                resolve(client(retryConfig))
              },
              reject,
            )
          })
        }

        // No refresh token or refresh failed — trigger full logout
        tokenManager.clearAll()
        if (window.location.pathname !== '/login') {
          window.dispatchEvent(new CustomEvent('auth:unauthorized'))
        }
      }

      return Promise.reject(error)
    }

    // 403 — dispatch forbidden event
    if (status === 403) {
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new CustomEvent('auth:forbidden'))
      }
      return Promise.reject(error)
    }

    return Promise.reject(error)
  },
)

export default client
