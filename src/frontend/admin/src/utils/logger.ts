export const logger = {
  warn(msg: string, data?: Record<string, unknown>) { console.warn({ level: 'warn', msg, data, ts: new Date().toISOString() }) },
  error(msg: string, data?: Record<string, unknown>) { console.error({ level: 'error', msg, data, ts: new Date().toISOString() }) },
}
