import { useEffect, useState, useCallback } from 'react'
import { Card, Form, Input, Button, Select, Avatar, message, Skeleton } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { useTranslation } from '../i18n'
import { getCurrentUser, updateNickname, updateProfile, type UserVO, type UpdateProfileRequest } from '../api/user'
import { SUPPORTED_LOCALES } from '../i18n/config'
import ErrorAlert from '../components/ErrorAlert'

const TIMEZONES = [
  'UTC', 'Asia/Shanghai', 'Asia/Tokyo', 'Asia/Seoul', 'Asia/Singapore',
  'America/New_York', 'America/Chicago', 'America/Los_Angeles',
  'Europe/London', 'Europe/Paris', 'Europe/Berlin',
  'Australia/Sydney', 'Pacific/Auckland',
]

const LOCALE_OPTIONS = SUPPORTED_LOCALES.map((l) => ({ value: l.code, label: l.label }))
const TIMEZONE_OPTIONS = TIMEZONES.map((tz) => ({ value: tz, label: tz }))

export default function Profile() {
  const { t } = useTranslation()
  const [form] = Form.useForm()

  const [user, setUser] = useState<UserVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchUser = useCallback(async () => {
    try {
      const res = await getCurrentUser()
      if (res.code === 0 && res.data) {
        setUser(res.data)
        form.setFieldsValue({
          nickname: res.data.nickname || '',
          realName: '',
          locale: 'zh-CN',
          timezone: 'Asia/Shanghai',
        })
      }
    } catch {
      setError(t('common.service_unavailable'))
    } finally {
      setLoading(false)
    }
  }, [t, form])

  useEffect(() => { fetchUser() }, [fetchUser])

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)

      if (values.nickname !== (user?.nickname || '')) {
        await updateNickname({ nickname: values.nickname })
      }

      const profileData: UpdateProfileRequest = {}
      if (values.realName) profileData.realName = values.realName
      if (values.locale) profileData.locale = values.locale
      if (values.timezone) profileData.timezone = values.timezone
      if (Object.keys(profileData).length > 0) {
        await updateProfile(profileData)
      }

      message.success(t('common.profile_save_success'))
      fetchUser()
    } catch {
      // form validation error
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Skeleton active paragraph={{ rows: 6 }} />
  if (error) return <ErrorAlert message={error} />

  const avatarUrl = user?.avatar || undefined
  const displayName = user?.nickname || user?.username || 'User'

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>{t('common.profile_title')}</h2>

      <Card bordered={false}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Avatar
            size={96}
            icon={<UserOutlined />}
            src={avatarUrl}
            style={{ marginBottom: 12 }}
          />
          <div style={{ fontSize: 18, fontWeight: 500 }}>{displayName}</div>
        </div>

        <Form
          form={form}
          layout="vertical"
          style={{ maxWidth: 480, margin: '0 auto' }}
        >
          <Form.Item
            name="nickname"
            label={t('common.profile_nickname')}
            rules={[
              { max: 64, message: '1-64 characters' },
            ]}
          >
            <Input placeholder={t('common.profile_nickname_placeholder')} />
          </Form.Item>

          <Form.Item
            name="realName"
            label={t('common.profile_real_name')}
          >
            <Input placeholder={t('common.profile_real_name_placeholder')} />
          </Form.Item>

          <Form.Item name="locale" label={t('common.profile_locale')}>
            <Select options={LOCALE_OPTIONS} />
          </Form.Item>

          <Form.Item name="timezone" label={t('common.profile_timezone')}>
            <Select showSearch options={TIMEZONE_OPTIONS} />
          </Form.Item>

          <Form.Item>
            <Button type="primary" onClick={handleSave} loading={saving} block>
              {t('common.save')}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
