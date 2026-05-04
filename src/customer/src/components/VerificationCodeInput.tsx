import { useRef, useEffect, useCallback, createRef } from 'react'
import { Input } from 'antd'
import type { InputRef } from 'antd'

interface VerificationCodeInputProps {
  value: string
  onChange: (code: string) => void
  length?: number
  disabled?: boolean
  error?: boolean
}

function focusInput(el: InputRef | null) {
  el?.focus?.()
}

export default function VerificationCodeInput({
  value,
  onChange,
  length = 6,
  disabled = false,
  error = false,
}: VerificationCodeInputProps) {
  const inputRefs = useRef<React.RefObject<InputRef>[]>(
    Array.from({ length }, () => createRef<InputRef>()),
  )

  useEffect(() => {
    focusInput(inputRefs.current[0]?.current)
  }, [])

  const handleChange = useCallback(
    (index: number, char: string) => {
      const digit = char.replace(/\D/g, '').slice(-1)
      if (!digit) return

      const newValue = value.split('')
      newValue[index] = digit
      const newCode = newValue.join('').slice(0, length)
      onChange(newCode)

      if (index < length - 1) {
        focusInput(inputRefs.current[index + 1]?.current)
      }
    },
    [value, onChange, length],
  )

  const handleKeyDown = useCallback(
    (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Backspace') {
        if (!value[index] && index > 0) {
          focusInput(inputRefs.current[index - 1]?.current)
        } else if (value[index]) {
          const newValue = value.split('')
          newValue[index] = ''
          onChange(newValue.join('').slice(0, length))
        }
      } else if (e.key === 'ArrowLeft' && index > 0) {
        focusInput(inputRefs.current[index - 1]?.current)
      } else if (e.key === 'ArrowRight' && index < length - 1) {
        focusInput(inputRefs.current[index + 1]?.current)
      }
    },
    [value, onChange, length],
  )

  const handlePaste = useCallback(
    (e: React.ClipboardEvent) => {
      e.preventDefault()
      const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length)
      onChange(pasted)
      const focusIndex = Math.min(pasted.length, length - 1)
      focusInput(inputRefs.current[focusIndex]?.current)
    },
    [onChange, length],
  )

  return (
    <div
      style={{ display: 'flex', gap: 8, justifyContent: 'center' }}
      onPaste={handlePaste}
    >
      {Array.from({ length }, (_, i) => (
        <Input
          key={i}
          ref={inputRefs.current[i]}
          className={error ? 'shake verification-code-segment' : 'verification-code-segment'}
          value={value[i] || ''}
          maxLength={1}
          disabled={disabled}
          status={error ? 'error' : undefined}
          onChange={(e) => handleChange(i, e.target.value)}
          onKeyDown={(e) => handleKeyDown(i, e)}
          aria-label={`Verification code, digit ${i + 1} of ${length}`}
          style={{
            width: 44,
            height: 44,
            textAlign: 'center',
            fontSize: 18,
            fontWeight: 600,
          }}
          autoComplete="off"
          inputMode="numeric"
          pattern="[0-9]"
        />
      ))}
    </div>
  )
}
