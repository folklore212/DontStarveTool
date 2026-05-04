import { LIMITS } from './constants'

export interface PasswordCheckResult {
  length: boolean
  hasUpper: boolean
  hasLower: boolean
  hasDigit: boolean
  hasSpecial: boolean
  noUsernameMatch: boolean
  noEmailMatch: boolean
}

/**
 * Special characters allowed in passwords — must match backend
 * PasswordComplexityValidator.java: @$!%*#?&
 */
const SPECIAL_CHARS_REGEX = /[@$!%*#?&]/

/**
 * Evaluates a password against all 7 rules from the backend validator.
 *
 * Rules (mirrors PasswordComplexityValidator.java):
 *   1. Length: 8–128 characters
 *   2-5. Character classes: at least 3 of 4 (upper, lower, digit, special)
 *   6. No 3+ consecutive chars from username (case-insensitive)
 *   7. No 3+ consecutive chars from email local part (case-insensitive)
 *
 * @param password  The password to evaluate
 * @param username  Optional username for substring check
 * @param email     Optional email for substring check
 */
export function evaluatePassword(
  password: string,
  username?: string,
  email?: string,
): PasswordCheckResult {
  if (!password) {
    return {
      length: false,
      hasUpper: false,
      hasLower: false,
      hasDigit: false,
      hasSpecial: false,
      noUsernameMatch: true,
      noEmailMatch: true,
    }
  }

  return {
    length: password.length >= LIMITS.PASSWORD_MIN && password.length <= LIMITS.PASSWORD_MAX,
    hasUpper: /[A-Z]/.test(password),
    hasLower: /[a-z]/.test(password),
    hasDigit: /[0-9]/.test(password),
    hasSpecial: SPECIAL_CHARS_REGEX.test(password),
    noUsernameMatch: username ? checkSubstring(password, username) : true,
    noEmailMatch: email ? checkEmailSubstring(password, email) : true,
  }
}

/**
 * Returns the number of character class rules that pass (0-4).
 * The backend requires at least 3 of 4.
 */
export function calculatePasswordScore(result: PasswordCheckResult): number {
  const classes = [result.hasUpper, result.hasLower, result.hasDigit, result.hasSpecial]
  const classCount = classes.filter(Boolean).length

  // Score = length (1 if ok) + class count (0-4) = 0-5
  // But backend only checks 3-of-4 for character classes + length
  // Return class count only, length is a hard requirement
  let score = classCount
  if (result.length) score = Math.min(4, score + 1)
  return score
}

export function getPasswordStrengthLabel(score: number): string {
  if (score <= 1) return 'auth.password_strength_weak'
  if (score === 2) return 'auth.password_strength_moderate'
  if (score === 3) return 'auth.password_strength_good'
  return 'auth.password_strength_strong'
}

export function getPasswordStrengthColor(score: number): string {
  if (score <= 1) return '#ff4d4f'
  if (score === 2) return '#faad14'
  if (score === 3) return '#1677ff'
  return '#52c41a'
}

/**
 * Checks if the password contains 3+ consecutive characters
 * that appear consecutively in the given reference string.
 * Both are lowercased before comparison.
 */
function checkSubstring(password: string, reference: string): boolean {
  const pw = password.toLowerCase()
  const ref = reference.toLowerCase()

  if (ref.length < 3) return true

  for (let i = 0; i <= ref.length - 3; i++) {
    const sub = ref.substring(i, i + 3)
    if (pw.includes(sub)) {
      return false
    }
  }

  return true
}

/**
 * Checks if the password contains 3+ consecutive characters
 * from the LOCAL PART of the email (before @).
 */
function checkEmailSubstring(password: string, email: string): boolean {
  const localPart = email.split('@')[0]
  if (!localPart || localPart.length < 3) return true
  return checkSubstring(password, localPart)
}

/**
 * Exported for direct use by unit tests.
 */
export { checkSubstring as checkUsernameSubstring, checkEmailSubstring }
