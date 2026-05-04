import axios from 'axios'

/**
 * Security note on token storage:
 *
 * Access tokens are stored in {@code localStorage} for SPA convenience.
 * This is a known trade-off: localStorage is readable by any JS running
 * on the same origin, making tokens vulnerable to XSS.  The following
 * layered mitigations are in place:
 *
 * 1. Short-lived access tokens (15 min) — limits the blast radius of a
 *    stolen token.
 * 2. {@code Content-Security-Policy} header (default-src 'self') in the
 *    backend's {@code WebMvcConfig.securityHeadersFilter()} — prevents
 *    inline script injection.
 * 3. In production, the reverse proxy should strip or reject
 *    untrusted scripts in transit.
 * 4. The {@code HttpOnly} cookie approach was deferred because the mobile
 *    client uses Bearer headers and we want a single auth mechanism.
 *
 * TODO: Consider migrating from localStorage to a BFF session-token proxy
 *       (Spring Session + Redis) when the project scope allows it.
 */
const client = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      }
    } else if (status === 403) {
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new CustomEvent('auth:forbidden'))
      }
    }
    return Promise.reject(error)
  },
)

export default client
