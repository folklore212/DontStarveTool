import { useState, useRef, useCallback, useEffect } from 'react'

/**
 * Generic countdown timer hook.
 * @param seconds Initial countdown duration in seconds
 */
export default function useCountdown(seconds: number) {
  const [count, setCount] = useState(0)
  const [isRunning, setIsRunning] = useState(false)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const start = useCallback(() => {
    clearTimer()
    setCount(seconds)
    setIsRunning(true)
    timerRef.current = setInterval(() => {
      setCount((prev) => {
        if (prev <= 1) {
          clearTimer()
          setIsRunning(false)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }, [seconds, clearTimer])

  const reset = useCallback(() => {
    clearTimer()
    setCount(0)
    setIsRunning(false)
  }, [clearTimer])

  // Cleanup on unmount
  useEffect(() => {
    return clearTimer
  }, [clearTimer])

  return { count, isRunning, start, reset }
}
