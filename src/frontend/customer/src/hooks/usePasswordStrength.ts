import { useState, useMemo, useRef, useEffect } from 'react'
import {
  evaluatePassword,
  calculatePasswordScore,
  getPasswordStrengthLabel,
  getPasswordStrengthColor,
} from '../utils/passwordValidator'
import type { PasswordCheckResult } from '../utils/passwordValidator'
import { TIMEOUTS } from '../utils/constants'

interface UsePasswordStrengthReturn {
  result: PasswordCheckResult
  score: number
  label: string
  color: string
}

/**
 * Real-time password strength evaluation hook.
 * Debounced to prevent excessive recalculations during typing.
 */
export default function usePasswordStrength(
  password: string,
  username?: string,
  email?: string,
): UsePasswordStrengthReturn {
  const [debouncedPassword, setDebouncedPassword] = useState(password)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastSetRef = useRef(password)

  useEffect(() => {
    timerRef.current = setTimeout(() => {
      if (lastSetRef.current !== password) {
        lastSetRef.current = password
        setDebouncedPassword(password)
      }
    }, TIMEOUTS.PASSWORD_STRENGTH_DEBOUNCE)

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [password])

  const result = useMemo(
    () => evaluatePassword(debouncedPassword, username, email),
    [debouncedPassword, username, email],
  )

  const score = calculatePasswordScore(result)
  const label = getPasswordStrengthLabel(score)
  const color = getPasswordStrengthColor(score)

  return { result, score, label, color }
}
