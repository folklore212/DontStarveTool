type LogLevel = 'debug' | 'info' | 'warn' | 'error'

interface LogEntry {
  level: LogLevel
  message: string
  data?: Record<string, unknown>
  timestamp: string
}

const isDev = import.meta.env.DEV

function formatEntry(level: LogLevel, message: string, data?: Record<string, unknown>): LogEntry {
  return {
    level,
    message,
    data,
    timestamp: new Date().toISOString(),
  }
}

function sanitizeData(data?: Record<string, unknown>): Record<string, unknown> | undefined {
  if (!data) return undefined
  const sanitized = { ...data }
  const piiKeys = ['password', 'credential', 'token', 'accessToken', 'refreshToken', 'mfaCode', 'code', 'secret', 'captchaOutput', 'passToken']
  for (const key of piiKeys) {
    if (key in sanitized) {
      sanitized[key] = '[REDACTED]'
    }
  }
  return sanitized
}

export const logger = {
  debug(message: string, data?: Record<string, unknown>): void {
    if (isDev) {
      console.debug(formatEntry('debug', message, sanitizeData(data)))
    }
  },

  info(message: string, data?: Record<string, unknown>): void {
    if (isDev) {
      console.info(formatEntry('info', message, sanitizeData(data)))
    }
  },

  warn(message: string, data?: Record<string, unknown>): void {
    console.warn(formatEntry('warn', message, sanitizeData(data)))
  },

  error(message: string, data?: Record<string, unknown>): void {
    console.error(formatEntry('error', message, sanitizeData(data)))
  },
}
