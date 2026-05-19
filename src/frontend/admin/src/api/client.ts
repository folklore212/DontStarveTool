import axios from 'axios'
import { tokenManager } from '../utils/tokenManager'
import { logger } from '../utils/logger'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const API_TIMEOUT = 10_000

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: { 'Content-Type': 'application/json' },
})

let isRefreshing = false
let pendingQueue: Array<{ resolve: (token: string) => void; reject: (e: unknown) => void }> = []

function onRefreshed(newToken: string) { pendingQueue.forEach(({ resolve }) => resolve(newToken)); pendingQueue = [] }
function onRefreshFailed(error: unknown) { pendingQueue.forEach(({ reject }) => reject(error)); pendingQueue = [] }

client.interceptors.request.use(
  (config) => {
    const token = tokenManager.getAccessToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error),
)

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!error.response) {
      if (error.code === 'ECONNABORTED') logger.warn('Request timed out', { url: error.config?.url })
      else logger.error('Network error', { url: error.config?.url, message: error.message })
      return Promise.reject(error)
    }
    const status = error.response.status
    if (status === 401) {
      const url = error.config?.url || ''
      const isAuthEndpoint = /\/auth\/(login|register|code|password|refresh|captcha-config)/.test(url)
      if (isAuthEndpoint) return Promise.reject(error)

      const refreshToken = tokenManager.getRefreshToken()
      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken }, { headers: { 'Content-Type': 'application/json' }, timeout: 5_000 })
          if (response.data?.code === 0) {
            const { accessToken, refreshToken: newRefresh, expiresIn } = response.data.data
            tokenManager.setAccessToken(accessToken)
            if (newRefresh) tokenManager.setRefreshToken(newRefresh)
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
        return new Promise((resolve, reject) => {
          pendingQueue.push({
            resolve: (token: string) => {
              const retryConfig = { ...error.config }
              retryConfig.headers.Authorization = `Bearer ${token}`
              resolve(client(retryConfig))
            },
            reject,
          })
        })
      }
      tokenManager.clearAll()
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      }
      return Promise.reject(error)
    }
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
