import { useState, useCallback } from 'react'
import { Form, Input, Button, message } from 'antd'
import { useNavigate, useLocation, Navigate } from 'react-router-dom'
import { useTranslation } from '../i18n'
import useFormError from '../hooks/useFormError'
import { resetPassword as resetPasswordApi } from '../api/auth'
import { evaluatePassword } from '../utils/passwordValidator'
import AuthCard from '../components/AuthCard'
import PasswordInput from '../components/PasswordInput'
import ErrorAlert from '../components/ErrorAlert'

interface LocationState {
  identifier: string
  code: string
}

interface ResetPasswordFormValues {
  newPassword: string
  confirmPassword: string
}

export default function PasswordResetPage() {
  const location = useLocation()
  const locState = location.state as LocationState | null
  const [form] = Form.useForm<ResetPasswordFormValues>()
  const [loading, setLoading] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const { t } = useTranslation()
  const { error, clearErrors, parseApiCode, parseApiError } = useFormError()
  const navigate = useNavigate()

  if (!locState || !locState.identifier || !locState.code) {
    return <Navigate to="/login" replace />
  }

  const { identifier, code } = locState

  const passwordComplexityValidator = useCallback(
    (_: unknown, value: string) => {
      if (!value) return Promise.resolve()
      const result = evaluatePassword(value)
      if (!result.length) {
        return Promise.reject(new Error(t('validation.password_min_length')))
      }
      const classCount = [result.hasUpper, result.hasLower, result.hasDigit, result.hasSpecial].filter(Boolean).length
      if (classCount < 3) {
        return Promise.reject(new Error(t('validation.password_complexity')))
      }
      return Promise.resolve()
    },
    [t],
  )

  const onFinish = async (values: ResetPasswordFormValues) => {
    clearErrors()
    setSubmitError(null)
    setLoading(true)

    try {
      const response = await resetPasswordApi({
        identifier,
        code,
        newPassword: values.newPassword,
      })

      if (response.code !== 0) {
        const msg = parseApiCode(response.code)
        setSubmitError(msg)
        setLoading(false)
        return
      }

      message.success(t('auth.password_reset_success'))
      navigate('/login')
    } catch (err: unknown) {
      parseApiError(err)
      setLoading(false)
    }
  }

  const onFinishFailed = () => {
    message.warning(t('validation.fix_errors'))
  }

  const displayError = submitError || error

  return (
    <AuthCard
      title={t('auth.reset_password_title')}
      subtitle={t('auth.reset_password_subtitle')}
    >
      {displayError && (
        <ErrorAlert key={displayError} message={t(displayError)} closable onClose={() => {
          clearErrors()
          setSubmitError(null)
        }} />
      )}

      <Form
        form={form}
        name="reset-password"
        onFinish={onFinish}
        onFinishFailed={onFinishFailed}
        size="large"
        layout="vertical"
      >
        <Form.Item
          name="newPassword"
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
            autoComplete="new-password"
            autoFocus
          />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: t('validation.required') },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
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

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} block>
            {t('auth.reset_password_button')}
          </Button>
        </Form.Item>
      </Form>
    </AuthCard>
  )
}
