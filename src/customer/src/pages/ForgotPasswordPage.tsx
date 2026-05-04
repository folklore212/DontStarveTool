import { useState, useRef, useEffect } from 'react'
import { Form, Input, Button } from 'antd'
import { MailOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from '../i18n'
import useFormError from '../hooks/useFormError'
import useCaptcha from '../hooks/useCaptcha'
import useCaptchaConfig from '../hooks/useCaptchaConfig'
import { sendCode } from '../api/auth'
import { normalizeEmail } from '../utils/sanitize'
import type { SendCodeRequest } from '../types/auth'
import AuthCard from '../components/AuthCard'
import CaptchaWidget from '../components/CaptchaWidget'
import type { CaptchaWidgetRef } from '../components/CaptchaWidget'
import ErrorAlert from '../components/ErrorAlert'
import LanguageSwitcher from '../components/LanguageSwitcher'

interface ForgotPasswordFormValues {
  identifier: string
}

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false)
  const { t } = useTranslation()
  const { error, clearErrors, parseApiError, parseApiCode } = useFormError()
  const captcha = useCaptcha('closed')
  const { registerCaptchaId, loading: captchaLoading } = useCaptchaConfig()
  const captchaRef = useRef<CaptchaWidgetRef>(null)
  const pendingSubmit = useRef<ForgotPasswordFormValues | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (captcha.captchaParams && pendingSubmit.current) {
      const values = pendingSubmit.current
      pendingSubmit.current = null
      doSubmit(values)
    }
  }, [captcha.captchaParams])

  const doSubmit = async (values: ForgotPasswordFormValues) => {
    clearErrors()
    setLoading(true)
    try {
      const email = normalizeEmail(values.identifier)
      const sendReq: SendCodeRequest = {
        identifier: email,
        identityType: 'EMAIL',
        purpose: 'RESET_PASSWORD',
        ...(captcha.captchaParams || {}),
      }
      const response = await sendCode(sendReq)
      if (response.code !== 0) {
        parseApiCode(response.code)
        captcha.reset()
        captchaRef.current?.reset()
        setLoading(false)
        return
      }
      navigate('/verify-email', {
        state: { identifier: email, purpose: 'RESET_PASSWORD', identityType: 'EMAIL' },
      })
    } catch (err: unknown) {
      parseApiError(err)
      captcha.reset()
      captchaRef.current?.reset()
      setLoading(false)
    }
  }

  const onFinish = async (values: ForgotPasswordFormValues) => {
    if (!captcha.captchaParams) {
      pendingSubmit.current = values
      captchaRef.current?.showCaptcha()
      return
    }
    doSubmit(values)
  }

  return (
    <AuthCard
      title={t('auth.forgot_password_title')}
      subtitle={t('auth.forgot_password_subtitle')}
    >
      <LanguageSwitcher />

      {error && (
        <ErrorAlert key={error} message={t(error)} closable onClose={clearErrors} />
      )}

      <Form name="forgot-password" onFinish={onFinish} size="large" layout="vertical">
        <Form.Item
          name="identifier"
          rules={[
            { required: true, message: t('validation.required') },
            { type: 'email', message: t('validation.email_invalid') },
          ]}
          validateTrigger={['onBlur']}
          getValueFromEvent={(e) => normalizeEmail(e.target.value)}
        >
          <Input
            prefix={<MailOutlined />}
            placeholder={t('auth.email_placeholder')}
            autoComplete="email"
            autoFocus
            aria-label={t('auth.email_label')}
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

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} block>
            {t('auth.send_code')}
          </Button>
        </Form.Item>
      </Form>

      <div className="auth-footer-link">
        <Link to="/login">{t('auth.back')}</Link>
      </div>
    </AuthCard>
  )
}
