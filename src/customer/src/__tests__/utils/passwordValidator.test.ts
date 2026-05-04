import { describe, it, expect } from 'vitest'
import { evaluatePassword, calculatePasswordScore, checkUsernameSubstring, checkEmailSubstring } from '../../utils/passwordValidator'

describe('evaluatePassword', () => {
  it('rejects empty password', () => {
    const result = evaluatePassword('')
    expect(result.length).toBe(false)
    expect(result.hasUpper).toBe(false)
    expect(result.hasLower).toBe(false)
    expect(result.hasDigit).toBe(false)
    expect(result.hasSpecial).toBe(false)
  })

  it('rejects password shorter than 8 characters', () => {
    const result = evaluatePassword('Ab1!')
    expect(result.length).toBe(false)
  })

  it('accepts password at minimum length (8)', () => {
    const result = evaluatePassword('Ab1!cdef')
    expect(result.length).toBe(true)
  })

  it('rejects password longer than 128 characters', () => {
    const pwd = 'A' + 'a'.repeat(128)
    const result = evaluatePassword(pwd)
    expect(result.length).toBe(false)
  })

  it('detects uppercase letters', () => {
    expect(evaluatePassword('abcdefgh').hasUpper).toBe(false)
    expect(evaluatePassword('Abcdefgh').hasUpper).toBe(true)
  })

  it('detects lowercase letters', () => {
    expect(evaluatePassword('ABCDEFGH').hasLower).toBe(false)
    expect(evaluatePassword('ABCDEFGh').hasLower).toBe(true)
  })

  it('detects digits', () => {
    expect(evaluatePassword('Abcdefgh').hasDigit).toBe(false)
    expect(evaluatePassword('Ab1defgh').hasDigit).toBe(true)
  })

  it('detects special characters from allowed set', () => {
    expect(evaluatePassword('Abc1efgh').hasSpecial).toBe(false)
    expect(evaluatePassword('Abc1@fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1!fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1#fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1$fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1%fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1&fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1*fgh').hasSpecial).toBe(true)
    expect(evaluatePassword('Abc1?fgh').hasSpecial).toBe(true)
  })

  it('detects username substring (3+ consecutive chars)', () => {
    const result = evaluatePassword('abc123!A', 'abc')
    expect(result.noUsernameMatch).toBe(false) // "abc" is in password
  })

  it('rejects username case-insensitively', () => {
    const result = evaluatePassword('ABC123!a', 'abc')
    expect(result.noUsernameMatch).toBe(false) // "ABC" matches "abc"
  })

  it('allows username shorter than 3 chars', () => {
    const result = evaluatePassword('abc123!A', 'ab')
    expect(result.noUsernameMatch).toBe(true)
  })

  it('detects email prefix in password', () => {
    const result = evaluatePassword('john123!A', undefined, 'john.doe@example.com')
    expect(result.noEmailMatch).toBe(false) // "john" is in password
  })

  it('allows password without email prefix match', () => {
    const result = evaluatePassword('secure123!A', undefined, 'john.doe@example.com')
    expect(result.noEmailMatch).toBe(true)
  })
})

describe('calculatePasswordScore', () => {
  it('returns 4 when all rules pass', () => {
    const result = evaluatePassword('Abc123!@')
    expect(calculatePasswordScore(result)).toBe(4)
  })

  it('returns 4 when 3 of 4 classes + length pass', () => {
    // 'Abcdefg1': upper(A) + lower(b-g) + digit(1) = 3 classes, length ok → 3+1=4
    const result = evaluatePassword('Abcdefg1')
    expect(calculatePasswordScore(result)).toBe(4)
  })

  it('returns 2 when 1 class + length passes', () => {
    // 'abcdefgh': lower only = 1 class, length ok → 1+1=2
    const result = evaluatePassword('abcdefgh')
    expect(calculatePasswordScore(result)).toBe(2)
  })

  it('returns 0 for empty password', () => {
    const result = evaluatePassword('')
    expect(calculatePasswordScore(result)).toBe(0)
  })
})

describe('checkUsernameSubstring', () => {
  it('returns false when password contains 3+ consecutive username chars', () => {
    expect(checkUsernameSubstring('abc123', 'abc')).toBe(false)
  })

  it('returns true when password does not contain username fragment', () => {
    expect(checkUsernameSubstring('xyz123', 'abc')).toBe(true)
  })

  it('handles case-insensitive comparison', () => {
    expect(checkUsernameSubstring('ABC123', 'abc')).toBe(false)
  })
})

describe('checkEmailSubstring', () => {
  it('extracts local part and checks substring', () => {
    expect(checkEmailSubstring('john123!', 'john.doe@example.com')).toBe(false)
  })

  it('returns true when email is not provided', () => {
    expect(checkEmailSubstring('john123!', '')).toBe(true)
  })
})
