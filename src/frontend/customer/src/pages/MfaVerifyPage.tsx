import { useState, useCallback } from 'react'
import { Button } from 'antd'
import { Link, useLocation, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from '../i18n'
import AuthCard from '../components/AuthCard'
import VerificationCodeInput from '../components/VerificationCodeInput'
import ErrorAlert from '../components/ErrorAlert'

interface MfaLocationState {
  mfaTypes: string[]
  loginIdentifier: string
  credential: string
  captchaParams?: {
    captchaOutput: string
    lotNumber: string
    passToken: string
    genTime: string
  } | null
}

export default function MfaVerifyPage() {
  const location = useLocation()
  const locState = location.state as MfaLocationState | null
  const [code, setCode] = useState('')
  const [useBackupCode, setUseBackupCode] = useState(false)
  const { completeMfa, state, clearError } = useAuth()
  const { t } = useTranslation()

  if (!locState) {
    return <Navigate to="/login" replace />
  }

  const handleVerify = useCallback(
    async (inputCode: string) => {
      if (inputCode.length < (useBackupCode ? 8 : 6)) return

      try {
        await completeMfa(inputCode)
      } catch {
        setCode('')
      }
    },
    [useBackupCode, completeMfa],
  )

  const displayError =
    state.status === 'error' && state.error
      ? t(state.error.message)
      : null

  const codeLength = useBackupCode ? 8 : 6

  return (
    <AuthCard
      title={t('auth.mfa_title')}
      subtitle={
        useBackupCode
          ? t('auth.mfa_backup_code_subtitle')
          : t('auth.mfa_subtitle')
      }
    >
      {displayError && (
        <ErrorAlert
          message={displayError}
          closable
          onClose={() => {
            clearError()
          }}
        />
      )}

      <div style={{ textAlign: 'center' }}>
        <VerificationCodeInput
          value={code}
          length={codeLength}
          onChange={(newCode) => {
            setCode(newCode)
            if (newCode.length === codeLength) {
              handleVerify(newCode)
            }
          }}
          disabled={state.status === 'loading'}
          error={!!displayError}
        />

        <Button
          type="link"
          style={{ marginTop: 24 }}
          onClick={() => {
            setUseBackupCode(!useBackupCode)
            setCode('')
            clearError()
          }}
        >
          {useBackupCode
            ? t('auth.mfa_totp_code')
            : t('auth.mfa_backup_code')}
        </Button>

        <Link
          to="/login"
          onClick={clearError}
          style={{ marginTop: 16, display: 'inline-block' }}
        >
          {t('auth.mfa_back_to_login')}
        </Link>
      </div>
    </AuthCard>
  )
}
