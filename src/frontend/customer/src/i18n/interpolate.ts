/**
 * Replaces {paramName} placeholders in a template string with values.
 * Missing params are left as-is in dev, replaced with '' in prod.
 *
 * Example:
 *   interpolate('Code sent to {email}', { email: 'j***@e***.com' })
 *   → 'Code sent to j***@e***.com'
 */
export function interpolate(
  template: string,
  params?: Record<string, string | number>,
): string {
  if (!params) return template

  const isDev = import.meta.env.DEV

  return template.replace(/\{(\w+)\}/g, (match, key: string) => {
    if (key in params) {
      return String(params[key])
    }
    return isDev ? match : ''
  })
}
