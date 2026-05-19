const isDev = typeof import.meta !== 'undefined' && (import.meta as any).env?.DEV

function sanitize(data?: Record<string, unknown>): Record<string, unknown> | undefined {
  if (!data) return undefined
  const s = { ...data }
  for (const k of ['password', 'token', 'accessToken', 'refreshToken', 'secret', 'code']) {
    if (k in s) s[k] = '[REDACTED]'
  }
  return s
}

export const logger = {
  debug(msg: string, data?: Record<string, unknown>) { if (isDev) console.debug({ level: 'debug', msg, data: sanitize(data), ts: new Date().toISOString() }) },
  warn(msg: string, data?: Record<string, unknown>) { console.warn({ level: 'warn', msg, data: sanitize(data), ts: new Date().toISOString() }) },
  error(msg: string, data?: Record<string, unknown>) { console.error({ level: 'error', msg, data: sanitize(data), ts: new Date().toISOString() }) },
}
