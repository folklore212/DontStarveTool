import { useEffect, useState, useRef } from 'react'
import { Form, Input, Button, Checkbox } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from '../i18n'
import useFormError from '../hooks/useFormError'
import useCaptcha from '../hooks/useCaptcha'
import useCaptchaConfig from '../hooks/useCaptchaConfig'
import { tokenManager } from '../utils/tokenManager'
import { login as loginApi } from '../api/auth'
import { extractErrorMessage, ERROR_CODE_I18N_MAP } from '../utils/errorHandler'
import type { LoginRequest } from '../types/auth'
import AuthCard from '../components/AuthCard'
import CaptchaWidget from '../components/CaptchaWidget'
import type { CaptchaWidgetRef } from '../components/CaptchaWidget'
import ErrorAlert from '../components/ErrorAlert'
import LanguageSwitcher from '../components/LanguageSwitcher'

interface LoginFormValues {
  identifier: string
  password: string
  mfaCode?: string
  rememberMe: boolean
}

export default function LoginPage() {
  const { loginCaptchaId, loading: captchaLoading } = useCaptchaConfig()
  const [form] = Form.useForm<LoginFormValues>()
  const [mfaRequired, setMfaRequired] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const { storeLoginResult, state, clearError } = useAuth()
  const { t } = useTranslation()
  const { error, clearErrors } = useFormError()
  const captcha = useCaptcha('open')
  const captchaRef = useRef<CaptchaWidgetRef>(null)
  const pendingSubmit = useRef<LoginFormValues | null>(null)

  const isLoading = state.status === 'loading'

  useEffect(() => {
    const saved = tokenManager.getRememberedIdentifier()
    if (saved) {
      form.setFieldsValue({ identifier: saved, rememberMe: true })
    }
  }, [form])

  // Auto-submit after captcha completes
  useEffect(() => {
    if (captcha.captchaParams && pendingSubmit.current) {
      const values = pendingSubmit.current
      pendingSubmit.current = null
      doSubmit(values)
    }
  }, [captcha.captchaParams])

  const resetCaptcha = () => {
    captcha.reset()
    captchaRef.current?.reset()
  }

  const doSubmit = async (values: LoginFormValues) => {
    clearErrors()
    setSubmitError(null)

    const loginRequest: LoginRequest = {
      identifier: values.identifier.trim(),
      credential: values.password,
      ...(mfaRequired && values.mfaCode ? { mfaCode: values.mfaCode } : {}),
      ...(captcha.captchaParams || {}),
    }

    try {
      const response = await loginApi(loginRequest)

      if (response.code !== 0) {
        const i18nKey = ERROR_CODE_I18N_MAP[response.code]
        setSubmitError(t(i18nKey || 'auth.error_unknown'))
        resetCaptcha()
        return
      }

      const data = response.data

      if (data.mfaRequired) {
        setMfaRequired(true)
        resetCaptcha()
        return
      }

      storeLoginResult(data.accessToken, data.refreshToken, data.expiresIn, data.userInfo)

      if (values.rememberMe) {
        tokenManager.setRememberedIdentifier(values.identifier.trim())
      } else {
        tokenManager.clearRememberedIdentifier()
      }
    } catch (err: unknown) {
      const extracted = extractErrorMessage(err)
      setSubmitError(t(extracted.i18nKey || extracted.message))
      resetCaptcha()
    }
  }

  const onFinish = async (values: LoginFormValues) => {
    // Trigger captcha if not yet completed
    if (!captcha.captchaParams) {
      pendingSubmit.current = values
      captchaRef.current?.showCaptcha()
      return
    }

    doSubmit(values)
  }

  const displayError = submitError || (error ? t(error) : null)
  const contextError = state.status === 'error' && state.error
    ? t(state.error.message)
    : null
  const showError = displayError || contextError

  return (
    <AuthCard
      title={t('auth.login_title')}
      subtitle={t('auth.login_subtitle')}
    >
      <LanguageSwitcher />

      {mfaRequired && (
        <ErrorAlert
          message={t('auth.mfa_subtitle')}
          type="info"
          closable={false}
        />
      )}

      {showError && (
        <ErrorAlert
          message={showError}
          closable
          onClose={() => {
            clearError()
            clearErrors()
            setSubmitError(null)
          }}
        />
      )}

      <Form
        form={form}
        name="login"
        onFinish={onFinish}
        size="large"
        layout="vertical"
        initialValues={{ rememberMe: false }}
      >
        <Form.Item
          name="identifier"
          rules={[
            { required: true, message: t('validation.required') },
            { min: 3, message: t('validation.username_min_length') },
            { max: 255 },
            { whitespace: true },
          ]}
        >
          <Input
            prefix={<UserOutlined />}
            placeholder={t('auth.identifier_placeholder')}
            autoComplete="username"
            id="login-identifier"
            autoFocus
            aria-label={t('auth.identifier_placeholder')}
            disabled={mfaRequired}
          />
        </Form.Item>

        <Form.Item
          name="password"
          rules={[
            { required: true, message: t('validation.required') },
            { min: 8, message: t('validation.password_min_length') },
          ]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder={t('auth.password_placeholder')}
            autoComplete="current-password"
            id="login-password"
            aria-label={t('auth.password_placeholder')}
          />
        </Form.Item>

        {mfaRequired && (
          <Form.Item
            name="mfaCode"
            rules={[
              { required: true, message: t('validation.required') },
              { len: 6, message: t('validation.code_length') },
            ]}
          >
            <Input
              prefix={<SafetyOutlined />}
              placeholder={t('auth.mfa_code_placeholder')}
              maxLength={6}
              autoComplete="one-time-code"
              inputMode="numeric"
              aria-label={t('auth.mfa_code_label')}
              autoFocus
            />
          </Form.Item>
        )}

        {loginCaptchaId && (
          <CaptchaWidget
            key={loginCaptchaId}
            ref={captchaRef}
            captchaId={loginCaptchaId}
            failMode="open"
            onCaptchaReady={captcha.handleCaptchaReady}
          />
        )}
        {captchaLoading && (
          <div style={{ textAlign: 'center', marginBottom: 16, color: '#999' }}>
            {t('common.captcha_loading')}
          </div>
        )}

        {!mfaRequired && (
          <Form.Item name="rememberMe" valuePropName="checked">
            <Checkbox>{t('auth.remember_me')}</Checkbox>
          </Form.Item>
        )}

        <Form.Item style={{ marginBottom: 8 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={isLoading}
            disabled={isLoading}
            block
          >
            {mfaRequired ? t('auth.mfa_verify') : t('auth.login_button')}
          </Button>
        </Form.Item>
      </Form>

      {!mfaRequired && (
        <div className="auth-links">
          <Link to="/forgot-password">{t('auth.forgot_password')}</Link>
          <Link to="/register">{t('auth.register_link')}</Link>
        </div>
      )}
    </AuthCard>
  )
}
