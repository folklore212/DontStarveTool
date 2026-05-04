/**
 * Trims leading and trailing whitespace from user input.
 */
export function trimInput(value: string): string {
  return value.trim()
}

/**
 * Normalizes an email address: trim + lowercase.
 * RFC 5321: local part is case-sensitive but in practice all major providers
 * treat it as case-insensitive.
 */
export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase()
}

/**
 * Masks an email address for display after sending a verification code.
 * Examples:
 *   "john.doe@example.com"  → "j***@e***.com"
 *   "a@b.com"               → "a***@b***.com"
 */
export function maskEmail(email: string): string {
  const [local, domain] = email.split('@')
  if (!local || !domain) return email

  const parts = domain.split('.')
  const tld = parts.pop()
  const domainName = parts.join('.')

  const maskedLocal = local[0] + '***' + (local.length > 1 ? '' : '')
  const maskedDomain = domainName[0] + '***'

  return `${maskedLocal}@${maskedDomain}.${tld}`
}

/**
 * Strips HTML tags from a string. Defense-in-depth measure —
 * React's JSX escaping handles this normally, but this is a safety net
 * for any text that might be injected into non-React contexts.
 */
export function stripHtml(input: string): string {
  return input.replace(/<[^>]*>/g, '')
}

/**
 * Normalizes a username: trim + preserve case (usernames are case-sensitive in this system).
 */
export function normalizeUsername(username: string): string {
  return username.trim()
}
