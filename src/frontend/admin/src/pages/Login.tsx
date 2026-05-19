import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, message, Typography } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import type { AxiosError } from 'axios'
import client from '../api/client'
import { useAuth } from '../context/AuthContext'
import useCaptcha from '../hooks/useCaptcha'
import useCaptchaConfig from '../hooks/useCaptchaConfig'
import CaptchaWidget from '../components/CaptchaWidget'
import type { CaptchaWidgetRef } from '../components/CaptchaWidget'

const { Title } = Typography

interface LoginFormValues {
  identifier: string
  password: string
  mfaCode?: string
}

interface LoginData {
  accessToken: string
  refreshToken?: string
  expiresIn?: number
}

function Login() {
  const [loading, setLoading] = useState(false)
  const [mfaRequired, setMfaRequired] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { login } = useAuth()
  const captcha = useCaptcha('open')
  const { captchaId, loading: captchaLoading } = useCaptchaConfig()
  const captchaRef = useRef<CaptchaWidgetRef>(null)
  const pendingSubmit = useRef<LoginFormValues | null>(null)

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
    setSubmitError(null)
    setLoading(true)
    try {
      const response = await client.post('/auth/login', {
        identifier: values.identifier,
        credential: values.password,
        mfaCode: values.mfaCode || undefined,
        ...(captcha.captchaParams || {}),
      })
      const apiData = response.data
      if (apiData.code !== 0) {
        setSubmitError(apiData.message || 'Login failed')
        resetCaptcha()
        return
      }
      const data = apiData.data as LoginData
      if ((data as any).mfaRequired) {
        setMfaRequired(true)
        resetCaptcha()
        setLoading(false)
        return
      }
      login(data.accessToken, data.refreshToken)
      message.success('Login successful')
      navigate('/')
    } catch (error: unknown) {
      const axiosError = error as AxiosError<{ message?: string }>
      setSubmitError(axiosError.response?.data?.message || 'Login failed')
      resetCaptcha()
    } finally {
      setLoading(false)
    }
  }

  const onFinish = async (values: LoginFormValues) => {
    if (!captcha.captchaParams) {
      pendingSubmit.current = values
      captchaRef.current?.showCaptcha()
      return
    }
    doSubmit(values)
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 400, boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 32 }}>
          Auth System Admin
        </Title>

        {submitError && (
          <div style={{ color: '#ff4d4f', marginBottom: 16, textAlign: 'center' }}>{submitError}</div>
        )}

        {mfaRequired && (
          <div style={{ color: '#1677ff', marginBottom: 16, textAlign: 'center' }}>MFA verification required</div>
        )}

        <Form name="login" onFinish={onFinish} size="large">
          <Form.Item name="identifier" rules={[{ required: true, message: 'Please enter your username or email' }]}>
            <Input
              prefix={<UserOutlined />}
              placeholder="Username or email"
              autoComplete="username"
              id="admin-login-identifier"
              autoFocus
              disabled={mfaRequired}
            />
          </Form.Item>

          <Form.Item name="password" rules={[{ required: true, message: 'Please enter your password' }]}>
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              autoComplete="current-password"
              id="admin-login-password"
            />
          </Form.Item>

          {mfaRequired && (
            <Form.Item name="mfaCode" rules={[{ required: true, message: 'MFA code required' }, { len: 6 }]}>
              <Input
                prefix={<SafetyOutlined />}
                placeholder="MFA code"
                maxLength={6}
                autoComplete="one-time-code"
                autoFocus
              />
            </Form.Item>
          )}

          {captchaId && <CaptchaWidget ref={captchaRef} captchaId={captchaId} failMode="open" onCaptchaReady={captcha.handleCaptchaReady} />}
          {captchaLoading && <div style={{ textAlign: 'center', marginBottom: 16, color: '#999' }}>Loading captcha...</div>}

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              {mfaRequired ? 'Verify MFA' : 'Log in'}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login
