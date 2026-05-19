import { useState, useCallback, useRef, useEffect, useMemo } from 'react'
import { Form, Input, Button, Checkbox, message } from 'antd'
import { MailOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from '../i18n'
import useFormError from '../hooks/useFormError'
import useCountdown from '../hooks/useCountdown'
import useCaptcha from '../hooks/useCaptcha'
import useCaptchaConfig from '../hooks/useCaptchaConfig'
import { register as registerApi, sendCode as sendCodeApi } from '../api/auth'
import { normalizeEmail } from '../utils/sanitize'
import { evaluatePassword } from '../utils/passwordValidator'
import type { SendCodeRequest, RegisterRequest } from '../types/auth'
import AuthCard from '../components/AuthCard'
import PasswordInput from '../components/PasswordInput'
import VerificationCodeInput from '../components/VerificationCodeInput'
import CaptchaWidget from '../components/CaptchaWidget'
import type { CaptchaWidgetRef } from '../components/CaptchaWidget'
import ErrorAlert from '../components/ErrorAlert'
import LanguageSwitcher from '../components/LanguageSwitcher'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

interface RegisterFormValues {
  email: string
  password: string
  confirmPassword: string
  verificationCode: string
  acceptTerms: boolean
}

/**
 * Generate a username from an email address.
 * Extracts the local part, lowercases, replaces invalid chars, truncates to 64.
 */
function generateUsername(email: string): string {
  const local = email.split('@')[0] || 'user'
  return local
    .toLowerCase()
    .replace(/[^a-z0-9_-]/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 64) || 'user'
}

export default function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const [loading, setLoading] = useState(false)
  const [codeSent, setCodeSent] = useState(false)
  const [contactValue, setContactValue] = useState('')
  const [codeValue, setCodeValue] = useState('')
  const [usernameRetry, setUsernameRetry] = useState(0)
  const { t } = useTranslation()
  const { error, fieldErrors, clearErrors, parseApiError, parseApiCode } = useFormError()
  const countdown = useCountdown(60)
  const captcha = useCaptcha('closed')
  const { registerCaptchaId, loading: captchaLoading } = useCaptchaConfig()
  const captchaRef = useRef<CaptchaWidgetRef>(null)
  const pendingSendCode = useRef(false)
  const navigate = useNavigate()

  // Auto-generate username from email, append retry suffix if needed
  const generatedUsername = useMemo(() => {
    if (!contactValue || !EMAIL_RE.test(contactValue)) return ''
    const base = generateUsername(contactValue)
    if (usernameRetry === 0) return base
    // Append 4-digit random suffix on retry
    const suffix = Math.floor(1000 + Math.random() * 9000)
    return `${base}_${suffix}`
  }, [contactValue, usernameRetry])

  const resetCaptcha = useCallback(() => {
    captcha.reset()
    captchaRef.current?.reset()
    pendingSendCode.current = false
  }, [captcha])

  const doSendCode = useCallback(async () => {
    clearErrors()
    if (!contactValue) {
      message.warning(t('validation.required'))
      return
    }

    const identifier = normalizeEmail(contactValue)
    const sendReq: SendCodeRequest = {
      identifier,
      identityType: 'EMAIL',
      purpose: 'REGISTER',
      ...(captcha.captchaParams || {}),
    }

    try {
      const response = await sendCodeApi(sendReq)
      if (response.code !== 0) {
        parseApiCode(response.code)
        resetCaptcha()
        return
      }
      setCodeSent(true)
      countdown.start()
      message.success(t('auth.code_sent', { email: identifier }))
      resetCaptcha()
    } catch (err: unknown) {
      parseApiError(err)
      resetCaptcha()
    }
  }, [contactValue, captcha.captchaParams, parseApiCode, parseApiError, countdown, t, resetCaptcha, clearErrors])

  // Auto-send code after captcha completes
  useEffect(() => {
    if (captcha.captchaParams && pendingSendCode.current) {
      pendingSendCode.current = false
      doSendCode()
    }
  }, [captcha.captchaParams, doSendCode])

  const handleSendCode = useCallback(() => {
    if (!captcha.captchaParams) {
      pendingSendCode.current = true
      captchaRef.current?.showCaptcha()
      return
    }
    doSendCode()
  }, [captcha.captchaParams, doSendCode])

  const onFinish = async (values: RegisterFormValues) => {
    if (codeValue.length !== 6) {
      message.warning(t('validation.code_length'))
      return
    }
    clearErrors()
    setLoading(true)

    try {
      const registerReq: RegisterRequest = {
        username: generatedUsername,
        email: normalizeEmail(values.email),
        password: values.password,
        identityType: 'EMAIL',
        verificationCode: codeValue,
      }

      const response = await registerApi(registerReq)

      if (response.code !== 0) {
        parseApiCode(response.code)
        // On username conflict, increment retry counter to generate a new suffix
        if (response.code === 40002) {
          setUsernameRetry((n) => n + 1)
        }
        setLoading(false)
        resetCaptcha()
        return
      }

      message.success(t('auth.registration_success'))
      navigate('/login')
    } catch (err: unknown) {
      parseApiError(err)
      setLoading(false)
      resetCaptcha()
    }
  }

  const passwordComplexityValidator = useCallback(
    (_: unknown, value: string) => {
      if (!value) return Promise.resolve()

      const result = evaluatePassword(value, generatedUsername, contactValue)

      if (!result.length) {
        return Promise.reject(new Error(t('validation.password_min_length')))
      }

      const classCount = [result.hasUpper, result.hasLower, result.hasDigit, result.hasSpecial].filter(Boolean).length
      if (classCount < 3) {
        return Promise.reject(new Error(t('validation.password_complexity')))
      }

      if (!result.noUsernameMatch && generatedUsername) {
        return Promise.reject(new Error(t('validation.password_username')))
      }

      if (!result.noEmailMatch) {
        return Promise.reject(new Error(t('validation.password_email')))
      }

      return Promise.resolve()
    },
    [generatedUsername, contactValue, t],
  )

  const fieldError = error || Object.values(fieldErrors)[0] || null

  return (
    <AuthCard
      title={t('auth.register_title')}
      subtitle={t('auth.register_subtitle')}
    >
      <LanguageSwitcher />

      {fieldError && (
        <ErrorAlert message={t(fieldError)} closable onClose={clearErrors} />
      )}

      <Form
        form={form}
        name="register"
        onFinish={onFinish}
        size="large"
        layout="vertical"
        initialValues={{ acceptTerms: false }}
      >
        {/* 手机注册暂未开放，保留以备后续启用
        <Form.Item style={{ marginBottom: 24 }}>
          <Segmented
            block
            size="large"
            value={registerMode}
            onChange={(val) => handleModeChange(val as RegisterMode)}
            options={[
              { label: t('auth.register_mode_email'), value: 'email', icon: <MailOutlined /> },
              { label: t('auth.register_mode_phone'), value: 'phone', icon: <PhoneOutlined /> },
            ]}
          />
        </Form.Item>
        */}

        <Form.Item
          name="email"
          rules={[
            { required: true, message: t('validation.required') },
            { pattern: EMAIL_RE, message: t('validation.email_invalid') },
          ]}
          validateTrigger={['onBlur']}
          getValueFromEvent={(e: React.ChangeEvent<HTMLInputElement>) => normalizeEmail(e.target.value)}
        >
          <Input
            prefix={<MailOutlined />}
            placeholder={t('auth.email_placeholder')}
            autoComplete="email"
            onChange={(e) => {
              setContactValue(e.target.value)
              if (usernameRetry > 0) setUsernameRetry(0)
              clearErrors()
            }}
            aria-label={t('auth.email_label')}
          />
        </Form.Item>

        <Form.Item
          name="password"
          rules={[
            { required: true, message: t('validation.required') },
            { min: 8, message: t('validation.password_min_length') },
            { max: 128, message: t('validation.password_max_length') },
            { validator: passwordComplexityValidator },
          ]}
        >
          <PasswordInput
            placeholder={t('auth.password_placeholder')}
            showStrengthBar
            username={generatedUsername}
            email={contactValue}
            autoComplete="new-password"
          />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          dependencies={['password']}
          rules={[
            { required: true, message: t('validation.required') },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error(t('validation.password_mismatch')))
              },
            }),
          ]}
        >
          <Input.Password
            placeholder={t('auth.confirm_password_placeholder')}
            autoComplete="new-password"
            aria-label={t('auth.confirm_password_label')}
          />
        </Form.Item>

        {registerCaptchaId && (
          <CaptchaWidget
            key={registerCaptchaId}
            ref={captchaRef}
            captchaId={registerCaptchaId}
            failMode="closed"
            onCaptchaReady={captcha.handleCaptchaReady}
          />
        )}
        {captchaLoading && (
          <div style={{ textAlign: 'center', marginBottom: 16, color: '#999' }}>
            {t('common.captcha_loading')}
          </div>
        )}

        <div style={{ marginBottom: 24, textAlign: 'center' }}>
          <VerificationCodeInput
            value={codeValue}
            onChange={(code) => setCodeValue(code)}
            disabled={loading}
            error={!!fieldErrors['code']}
          />
          {fieldErrors['code'] && (
            <div style={{ color: '#ff4d4f', fontSize: 13, marginTop: 4 }}>
              {t(fieldErrors['code'])}
            </div>
          )}
          <div style={{ marginTop: 12 }}>
            {countdown.isRunning ? (
              <Button disabled block>
                {t('auth.resend_in', { seconds: countdown.count })}
              </Button>
            ) : (
              <Button
                onClick={handleSendCode}
                disabled={countdown.isRunning}
                block
              >
                {codeSent ? t('auth.resend_code') : t('auth.send_code')}
              </Button>
            )}
          </div>
        </div>

        <Form.Item
          name="acceptTerms"
          valuePropName="checked"
          rules={[
            {
              validator: (_, value) =>
                value
                  ? Promise.resolve()
                  : Promise.reject(new Error(t('validation.accept_terms'))),
            },
          ]}
        >
          <Checkbox>
            {t('auth.accept_terms_label')}{' '}
            <Link to="/terms">{t('auth.terms_of_service')}</Link>
            {' '}{t('auth.and')}{' '}
            <Link to="/privacy">{t('auth.privacy_policy')}</Link>
          </Checkbox>
        </Form.Item>

        <Form.Item style={{ marginBottom: 8 }}>
          <Button type="primary" htmlType="submit" loading={loading} block>
            {t('auth.register_button')}
          </Button>
        </Form.Item>
      </Form>

      <div className="auth-footer-link">
        <Link to="/login">{t('auth.login_link')}</Link>
      </div>
    </AuthCard>
  )
}
