const isDev = typeof import.meta !== 'undefined' && (import.meta as any).env?.DEV

export const logger = {
  warn(msg: string, data?: Record<string, unknown>) { console.warn({ level: 'warn', msg, data, ts: new Date().toISOString() }) },
  error(msg: string, data?: Record<string, unknown>) { console.error({ level: 'error', msg, data, ts: new Date().toISOString() }) },
}
