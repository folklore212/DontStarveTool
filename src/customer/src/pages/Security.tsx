import { useEffect, useState, useCallback } from 'react'
import { Card, Form, Input, Button, Modal, Tag, Image, Typography, message, Skeleton, Row, Col, Space } from 'antd'
import {
  SafetyOutlined,
  CopyOutlined,
  DownloadOutlined,
  DeleteOutlined,
  LockOutlined,
  KeyOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  ExclamationCircleOutlined,
  QrcodeOutlined,
} from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from '../i18n'
import {
  getMfaStatus,
  initMfaSetup,
  verifyMfaSetup,
  disableMfa,
  getBackupCodes,
  type MfaStatusVO,
  type MfaSetupInitResponse,
} from '../api/mfa'
import { changePassword } from '../api/auth'
import ErrorAlert from '../components/ErrorAlert'

const { Text, Paragraph, Title } = Typography

export default function Security() {
  const { logout } = useAuth()
  const { t } = useTranslation()

  const [mfaStatus, setMfaStatus] = useState<MfaStatusVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordForm] = Form.useForm()

  const [mfaSetupOpen, setMfaSetupOpen] = useState(false)
  const [mfaSetupData, setMfaSetupData] = useState<MfaSetupInitResponse | null>(null)
  const [mfaVerifying, setMfaVerifying] = useState(false)
  const [mfaCode, setMfaCode] = useState('')
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null)

  const [mfaDisableOpen, setMfaDisableOpen] = useState(false)
  const [mfaDisablePassword, setMfaDisablePassword] = useState('')
  const [mfaDisabling, setMfaDisabling] = useState(false)

  const fetchData = useCallback(async () => {
    try {
      const res = await getMfaStatus()
      if (res.code === 0) setMfaStatus(res.data || [])
    } catch {
      setError(t('common.service_unavailable'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => { fetchData() }, [fetchData])

  const mfaEnabled = mfaStatus.some((m) => m.enabled)

  const handlePasswordChange = async (values: { oldPassword: string; newPassword: string }) => {
    setPasswordSaving(true)
    try {
      const res = await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
      if (res.code !== 0) {
        message.error(res.message || t('common.internal_error'))
        return
      }
      message.success(t('common.security_password_success'))
      passwordForm.resetFields()
      setTimeout(() => logout(), 1500) // brief delay so user sees the success message
    } catch {
      message.error(t('common.service_unavailable'))
    } finally {
      setPasswordSaving(false)
    }
  }

  const handleStartMfaSetup = async () => {
    try {
      const res = await initMfaSetup('TOTP')
      if (res.code !== 0) { message.error(res.message || t('common.internal_error')); return }
      setMfaSetupData(res.data)
      setBackupCodes(null)
      setMfaCode('')
      setMfaSetupOpen(true)
    } catch { message.error(t('common.service_unavailable')) }
  }

  const handleVerifyMfa = async () => {
    if (mfaCode.length !== 6) return
    setMfaVerifying(true)
    try {
      const res = await verifyMfaSetup(mfaCode)
      if (res.code !== 0) { message.error(res.message || t('common.internal_error')); setMfaVerifying(false); return }
      const codesRes = await getBackupCodes()
      setBackupCodes(codesRes.data || [])
      message.success(t('common.security_mfa_setup_success'))
      fetchData()
    } catch { message.error(t('common.service_unavailable')) }
    finally { setMfaVerifying(false) }
  }

  const handleCloseMfaSetup = () => {
    setMfaSetupOpen(false); setMfaSetupData(null); setBackupCodes(null); setMfaCode('')
  }

  const handleDisableMfa = async () => {
    setMfaDisabling(true)
    try {
      const res = await disableMfa(mfaDisablePassword)
      if (res.code !== 0) { message.error(res.message || t('common.internal_error')); setMfaDisabling(false); return }
      message.success(t('common.security_mfa_disabled_success'))
      setMfaDisableOpen(false); setMfaDisablePassword('')
      fetchData()
    } catch { message.error(t('common.service_unavailable')) }
    finally { setMfaDisabling(false) }
  }

  const handleExportData = async () => {
    try {
      const { exportData } = await import('../api/auth')
      const res = await exportData()
      if (res.code === 0) {
        const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a'); a.href = url; a.download = 'my-data.json'; a.click()
        URL.revokeObjectURL(url)
        message.success(t('common.security_export_success'))
      }
    } catch { message.error(t('common.service_unavailable')) }
  }

  const handleDeleteAccount = () => {
    Modal.confirm({
      title: t('common.security_delete_account'),
      icon: <ExclamationCircleOutlined />,
      content: t('common.security_delete_confirm'),
      okText: t('common.confirm'), okType: 'danger', cancelText: t('common.cancel'),
      onOk: async () => {
        try {
          const { forgetMe } = await import('../api/auth')
          await forgetMe()
          message.success(t('common.security_delete_success'))
          logout()
        } catch { message.error(t('common.service_unavailable')) }
      },
    })
  }

  const copyText = (text: string) => {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => message.success(t('common.copied')))
    } else {
      const ta = document.createElement('textarea'); ta.value = text
      ta.style.position = 'fixed'; ta.style.opacity = '0'
      document.body.appendChild(ta); ta.select()
      document.execCommand('copy'); document.body.removeChild(ta)
      message.success(t('common.copied'))
    }
  }

  if (loading) return <Skeleton active paragraph={{ rows: 6 }} />
  if (error) return <ErrorAlert message={error} />

  return (
    <div>
      <Title level={3} style={{ marginBottom: 24 }}>{t('common.security_title')}</Title>

      {/* Password Change */}
      <Card
        bordered={false}
        style={{ marginBottom: 24 }}
        title={
          <Space><LockOutlined style={{ color: '#1677ff' }} />{t('common.security_password')}</Space>
        }
      >
        <Paragraph type="secondary" style={{ marginBottom: 24 }}>{t('common.security_password_desc')}</Paragraph>
        <Form form={passwordForm} layout="vertical" style={{ maxWidth: 420 }} onFinish={handlePasswordChange}>
          <Form.Item name="oldPassword" label={t('common.security_old_password')} rules={[{ required: true }]}>
            <Input.Password prefix={<KeyOutlined />} />
          </Form.Item>
          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item name="newPassword" label={t('common.security_new_password')} rules={[{ required: true, min: 8 }]}>
                <Input.Password />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="confirmPassword" label={t('common.security_confirm_password')} dependencies={['newPassword']}
                rules={[{ required: true }, ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                    return Promise.reject(new Error('Passwords do not match'))
                  },
                })]}
              >
                <Input.Password />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={passwordSaving}>{t('common.save')}</Button>
          </Form.Item>
        </Form>
      </Card>

      {/* MFA */}
      <Card
        bordered={false}
        style={{ marginBottom: 24 }}
        title={
          <Space><SafetyOutlined style={{ color: '#1677ff' }} />{t('common.security_mfa')}</Space>
        }
        extra={
          <Space>
            {mfaEnabled
              ? <Tag icon={<CheckCircleFilled />} color="success">{t('common.dashboard_mfa_enabled')}</Tag>
              : <Tag icon={<CloseCircleFilled />} color="warning">{t('common.dashboard_mfa_disabled')}</Tag>
            }
          </Space>
        }
      >
        <Paragraph type="secondary" style={{ marginBottom: 24 }}>{t('common.security_mfa_desc')}</Paragraph>
        {!mfaEnabled ? (
          <Button type="primary" icon={<SafetyOutlined />} onClick={handleStartMfaSetup}>
            {t('common.security_mfa_enable')}
          </Button>
        ) : (
          <Button danger onClick={() => setMfaDisableOpen(true)}>
            {t('common.security_mfa_disable')}
          </Button>
        )}
      </Card>

      {/* Account Operations */}
      <Card
        bordered={false}
        title={
          <Space><DeleteOutlined style={{ color: '#ff4d4f' }} />{t('common.security_account')}</Space>
        }
      >
        <Paragraph type="secondary" style={{ marginBottom: 24 }}>{t('common.security_account_desc')}</Paragraph>
        <Space>
          <Button icon={<DownloadOutlined />} onClick={handleExportData}>{t('common.security_export_data')}</Button>
          <Button danger icon={<DeleteOutlined />} onClick={handleDeleteAccount}>{t('common.security_delete_account')}</Button>
        </Space>
      </Card>

      {/* MFA Setup Modal */}
      <Modal title={t('common.security_mfa_setup_title')} open={mfaSetupOpen} onCancel={handleCloseMfaSetup} footer={null} width={480}>
        {backupCodes ? (
          <div style={{ textAlign: 'center' }}>
            <CheckCircleFilled style={{ fontSize: 48, color: '#52c41a', marginBottom: 16 }} />
            <Title level={4}>{t('common.security_mfa_backup_codes_title')}</Title>
            <Paragraph type="secondary">{t('common.security_mfa_backup_codes_desc')}</Paragraph>
            <div style={{
              display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 24px',
              background: '#fafafa', padding: '20px 24px', borderRadius: 8,
              fontFamily: 'monospace', fontSize: 15, marginBottom: 20, maxWidth: 360, margin: '0 auto 20px',
            }}>
              {backupCodes.map((c, i) => <div key={i}>{c}</div>)}
            </div>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button icon={<CopyOutlined />} onClick={() => copyText(backupCodes.join('\n'))} block>
                {t('common.copy')}
              </Button>
              <Button type="primary" onClick={handleCloseMfaSetup} block>{t('common.close')}</Button>
            </Space>
          </div>
        ) : mfaSetupData ? (
          <div style={{ textAlign: 'center' }}>
            <QrcodeOutlined style={{ fontSize: 48, color: '#1677ff', marginBottom: 16 }} />
            <Title level={4}>{t('common.security_mfa_setup_step1')}</Title>
            <Image src={mfaSetupData.qrCodeUri} alt="QR Code" width={200} style={{ marginBottom: 16 }} />
            <Paragraph>{t('common.security_mfa_setup_step2')}</Paragraph>
            <div style={{
              background: '#fafafa', padding: '10px 16px', borderRadius: 4,
              fontFamily: 'monospace', marginBottom: 20, wordBreak: 'break-all',
            }}>
              <Text copyable>{mfaSetupData.secret}</Text>
            </div>
            <Paragraph>{t('common.security_mfa_setup_step3')}</Paragraph>
            <Input
              maxLength={6} size="large"
              placeholder={t('common.security_mfa_code_placeholder')}
              value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, ''))}
              style={{ textAlign: 'center', fontSize: 20, letterSpacing: 8, marginBottom: 16 }}
            />
            <Button type="primary" size="large" onClick={handleVerifyMfa} loading={mfaVerifying}
              disabled={mfaCode.length !== 6} block>
              {t('common.confirm')}
            </Button>
          </div>
        ) : null}
      </Modal>

      {/* MFA Disable Modal */}
      <Modal
        title={t('common.security_mfa_disable')}
        open={mfaDisableOpen}
        onCancel={() => { setMfaDisableOpen(false); setMfaDisablePassword('') }}
        onOk={handleDisableMfa}
        confirmLoading={mfaDisabling}
        okType="danger"
        okText={t('common.confirm')}
      >
        <Paragraph>{t('common.security_mfa_disable_confirm')}</Paragraph>
        <Input.Password
          placeholder={t('common.security_mfa_disable_password')}
          value={mfaDisablePassword}
          onChange={(e) => setMfaDisablePassword(e.target.value)}
        />
      </Modal>
    </div>
  )
}
