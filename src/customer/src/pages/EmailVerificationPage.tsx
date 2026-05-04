import { useState, useCallback } from 'react'
import { Button, message } from 'antd'
import { useNavigate, useLocation, Navigate } from 'react-router-dom'
import { useTranslation } from '../i18n'
import useFormError from '../hooks/useFormError'
import useCountdown from '../hooks/useCountdown'
import { verifyCode, sendCode } from '../api/auth'
import { maskEmail } from '../utils/sanitize'
import { ErrorCode } from '../types/errors'
import type { SendCodeRequest } from '../types/auth'
import AuthCard from '../components/AuthCard'
import VerificationCodeInput from '../components/VerificationCodeInput'
import ErrorAlert from '../components/ErrorAlert'

interface LocationState {
  identifier: string
  purpose: 'REGISTER' | 'RESET_PASSWORD' | 'ACTIVATE'
  identityType: string
}

export default function EmailVerificationPage() {
  const location = useLocation()
  const locState = location.state as LocationState | null
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [attempts, setAttempts] = useState(0)
  const { t } = useTranslation()
  const { error, clearErrors, parseApiError, parseApiCode } = useFormError()
  const countdown = useCountdown(60)
  const navigate = useNavigate()

  if (!locState) {
    return <Navigate to="/login" replace />
  }

  const { identifier, purpose, identityType } = locState

  const handleVerify = useCallback(async (inputCode: string) => {
    if (inputCode.length !== 6) return
    clearErrors()
    setLoading(true)

    try {
      const response = await verifyCode({
        identifier,
        code: inputCode,
        purpose,
      })

      if (response.code !== 0) {
        parseApiCode(response.code)
        setCode('')
        setAttempts((prev) => prev + 1)
        setLoading(false)

        // Auto-trigger resend on expired code
        if (response.code === ErrorCode.VERIFICATION_CODE_EXPIRED) {
          handleResend()
        }
        return
      }

      if (purpose === 'REGISTER' || purpose === 'ACTIVATE') {
        message.success(t('auth.registration_success'))
        navigate('/login')
      } else if (purpose === 'RESET_PASSWORD') {
        navigate('/reset-password', { state: { identifier, code: inputCode } })
      }
    } catch (err: unknown) {
      parseApiError(err)
      setCode('')
      setAttempts((prev) => prev + 1)
      setLoading(false)
    }
  }, [identifier, purpose, parseApiError, countdown, navigate, t])

  const handleResend = useCallback(async () => {
    try {
      const sendReq: SendCodeRequest = {
        identifier,
        identityType: identityType || 'EMAIL',
        purpose,
      }

      const response = await sendCode(sendReq)

      if (response.code !== 0) {
        parseApiCode(response.code)
        return
      }

      countdown.start()
      message.success(t('auth.code_sent', { email: maskEmail(identifier) }))
    } catch (err: unknown) {
      parseApiError(err)
    }
  }, [identifier, identityType, purpose, parseApiError, countdown, t])

  const maxAttemptsExceeded = attempts >= 3

  return (
    <AuthCard title={t('auth.verify_email_title')}>
      {error && (
        <ErrorAlert
          key={error}
          message={t(error)}
          closable
          onClose={clearErrors}
        />
      )}

      <div style={{ textAlign: 'center' }}>
        <p style={{ color: 'var(--color-text-secondary)', marginBottom: 24 }}>
          {t('auth.code_sent', { email: maskEmail(identifier) })}
        </p>

        <VerificationCodeInput
          value={code}
          onChange={(newCode) => {
            setCode(newCode)
            if (newCode.length === 6) {
              handleVerify(newCode)
            }
          }}
          disabled={loading || maxAttemptsExceeded}
          error={!!error}
        />

        <div style={{ marginTop: 24 }}>
          {maxAttemptsExceeded ? (
            <p style={{ color: 'var(--color-error)' }}>
              {t('auth.error_code_max_attempts')}
            </p>
          ) : (
            <p>{t('auth.no_code_received')}</p>
          )}

          {countdown.isRunning ? (
            <Button disabled block style={{ marginTop: 8 }}>
              {t('auth.resend_in', { seconds: countdown.count })}
            </Button>
          ) : (
            <Button
              onClick={handleResend}
              block
              style={{ marginTop: 8 }}
              disabled={maxAttemptsExceeded}
            >
              {t('auth.resend_code')}
            </Button>
          )}
        </div>
      </div>
    </AuthCard>
  )
}
