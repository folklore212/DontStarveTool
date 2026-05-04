import { describe, it, expect } from 'vitest'
import { trimInput, normalizeEmail, maskEmail, stripHtml, normalizeUsername } from '../../utils/sanitize'

describe('trimInput', () => {
  it('trims leading and trailing whitespace', () => {
    expect(trimInput('  hello  ')).toBe('hello')
  })

  it('preserves internal whitespace', () => {
    expect(trimInput('  hello world  ')).toBe('hello world')
  })
})

describe('normalizeEmail', () => {
  it('trims and lowercases', () => {
    expect(normalizeEmail('  John.Doe@EXAMPLE.COM  ')).toBe('john.doe@example.com')
  })
})

describe('maskEmail', () => {
  it('masks standard email', () => {
    expect(maskEmail('john.doe@example.com')).toBe('j***@e***.com')
  })

  it('handles single character local part', () => {
    expect(maskEmail('a@b.com')).toBe('a***@b***.com')
  })
})

describe('stripHtml', () => {
  it('removes HTML tags', () => {
    expect(stripHtml('<script>alert("xss")</script>')).toBe('alert("xss")')
  })

  it('preserves non-HTML text', () => {
    expect(stripHtml('Hello, World!')).toBe('Hello, World!')
  })
})

describe('normalizeUsername', () => {
  it('trims whitespace', () => {
    expect(normalizeUsername('  myuser  ')).toBe('myuser')
  })

  it('preserves case', () => {
    expect(normalizeUsername('MyUser')).toBe('MyUser')
  })
})
