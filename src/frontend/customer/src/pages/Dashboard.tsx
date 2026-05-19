import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Row, Col, Statistic, Tag, Button, Descriptions, Space, Skeleton } from 'antd'
import {
  UserOutlined,
  MailOutlined,
  SafetyOutlined,
  ClockCircleOutlined,
  EditOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'
import { useTranslation } from '../i18n'
import { getCurrentUser, type UserVO } from '../api/user'
import { getMfaStatus, type MfaStatusVO } from '../api/mfa'
import ErrorAlert from '../components/ErrorAlert'

export default function Dashboard() {
  const { state } = useAuth()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const [user, setUser] = useState<UserVO | null>(null)
  const [mfaStatus, setMfaStatus] = useState<MfaStatusVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    try {
      const [userRes, mfaRes] = await Promise.all([
        getCurrentUser(),
        getMfaStatus(),
      ])
      if (userRes.code === 0) setUser(userRes.data)
      if (mfaRes.code === 0) setMfaStatus(mfaRes.data || [])
      setError(null)
    } catch {
      setError(t('common.service_unavailable'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => { fetchData() }, [fetchData])

  const mfaEnabled = mfaStatus.some((m) => m.enabled)
  const displayName = state.userInfo?.nickname || state.userInfo?.username || 'User'

  if (loading) return <Skeleton active paragraph={{ rows: 6 }} />
  if (error) return <ErrorAlert message={error} />

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>
        {t('common.dashboard_welcome', { name: displayName })}
      </h2>

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <Card title={displayName} bordered={false}>
            <Descriptions column={1} size="middle">
              <Descriptions.Item label={<><UserOutlined /> {t('common.dashboard_user_id')}</>}>
                {user?.userId}
              </Descriptions.Item>
              <Descriptions.Item label={<><MailOutlined /> {t('common.dashboard_email')}</>}>
                {user?.email || '-'}
              </Descriptions.Item>
              <Descriptions.Item label={t('common.dashboard_roles')}>
                <Space>
                  {(state.userInfo?.roles || []).map((r) => (
                    <Tag key={r} color="blue">{r}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label={<><ClockCircleOutlined /> {t('common.dashboard_last_login')}</>}>
                {user?.lastLoginAt || '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card bordered={false}>
            <Statistic
              title={t('common.dashboard_mfa_status')}
              value={mfaEnabled ? t('common.dashboard_mfa_enabled') : t('common.dashboard_mfa_disabled')}
              valueStyle={{ color: mfaEnabled ? '#52c41a' : '#faad14' }}
              prefix={<SafetyOutlined />}
            />
          </Card>

          <Card title={t('common.dashboard_quick_actions')} bordered={false} style={{ marginTop: 24 }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" icon={<EditOutlined />} block onClick={() => navigate('/profile')}>
                {t('common.dashboard_edit_profile')}
              </Button>
              <Button icon={<SettingOutlined />} block onClick={() => navigate('/security')}>
                {t('common.dashboard_security_settings')}
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
