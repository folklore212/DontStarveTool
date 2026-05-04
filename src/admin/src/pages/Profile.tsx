import { useEffect, useState } from 'react'
import {
  Card,
  Descriptions,
  Button,
  Form,
  Input,
  message,
  Spin,
  Typography,
  Tag,
  Divider,
} from 'antd'
import { EditOutlined, LockOutlined, UserOutlined } from '@ant-design/icons'
import type { UserVO } from '../types'
import * as profileApi from '../api/profile'
import { changePassword } from '../api/auth'

const { Title } = Typography

function Profile() {
  const [loading, setLoading] = useState(true)
  const [user, setUser] = useState<UserVO | null>(null)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [pwChanging, setPwChanging] = useState(false)
  const [pwSaving, setPwSaving] = useState(false)
  const [profileForm] = Form.useForm()
  const [pwForm] = Form.useForm()

  const fetchProfile = async () => {
    setLoading(true)
    try {
      const res = await profileApi.getCurrentUser()
      setUser(res.data.data ?? null)
    } catch {
      message.error('Failed to load profile')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProfile() }, [])

  const handleSaveProfile = async (values: Record<string, unknown>) => {
    setSaving(true)
    try {
      await profileApi.updateProfile(values)
      message.success('Profile updated')
      setEditing(false)
      fetchProfile()
    } catch {
      message.error('Failed to update profile')
    } finally {
      setSaving(false)
    }
  }

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string }) => {
    setPwSaving(true)
    try {
      await changePassword(values.oldPassword, values.newPassword)
      message.success('Password changed successfully')
      pwForm.resetFields()
      setPwChanging(false)
    } catch {
      message.error('Failed to change password')
    } finally {
      setPwSaving(false)
    }
  }

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />

  return (
    <Card>
      <Title level={2}>
        <UserOutlined style={{ marginRight: 8 }} />
        Profile
      </Title>

      {user && !editing && (
        <>
          <Descriptions bordered column={2} style={{ maxWidth: 600 }}>
            <Descriptions.Item label="User ID">{user.userId}</Descriptions.Item>
            <Descriptions.Item label="Username">{user.username}</Descriptions.Item>
            <Descriptions.Item label="Email">{user.email || '-'}</Descriptions.Item>
            <Descriptions.Item label="Phone">{user.phone || '-'}</Descriptions.Item>
            <Descriptions.Item label="Nickname">{user.nickname || '-'}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={user.status === 1 ? 'green' : 'red'}>
                {user.status === 1 ? 'Active' : 'Disabled'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Last Login">{user.lastLoginAt || '-'}</Descriptions.Item>
            <Descriptions.Item label="Last Login IP">{user.lastLoginIp || '-'}</Descriptions.Item>
            <Descriptions.Item label="Password Changed">{user.passwordChangedAt || '-'}</Descriptions.Item>
            <Descriptions.Item label="Created">{user.createdAt}</Descriptions.Item>
          </Descriptions>
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={async () => {
              try {
                const res = await profileApi.getCurrentProfile()
                const p = res.data.data
                profileForm.setFieldsValue({
                  realName: p?.realName || '',
                  locale: p?.locale || '',
                  timezone: p?.timezone || '',
                })
              } catch {
                // pre-fill is best-effort; form remains empty
              }
              setEditing(true)
            }}
            style={{ marginTop: 16 }}
          >
            Edit Profile
          </Button>
        </>
      )}

      {editing && (
        <Form
          form={profileForm}
          layout="vertical"
          style={{ maxWidth: 400 }}
          onFinish={handleSaveProfile}
        >
          <Form.Item name="realName" label="Real Name">
            <Input placeholder="Your full name" />
          </Form.Item>
          <Form.Item name="locale" label="Locale">
            <Input placeholder="e.g. zh-CN, en-US" />
          </Form.Item>
          <Form.Item name="timezone" label="Timezone">
            <Input placeholder="e.g. Asia/Shanghai" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={saving}>Save</Button>
            <Button
              style={{ marginLeft: 8 }}
              onClick={() => { setEditing(false); profileForm.resetFields() }}
            >
              Cancel
            </Button>
          </Form.Item>
        </Form>
      )}

      <Divider />

      <Title level={4}>
        <LockOutlined style={{ marginRight: 8 }} />
        Change Password
      </Title>

      {pwChanging ? (
        <Form
          form={pwForm}
          layout="vertical"
          style={{ maxWidth: 400 }}
          onFinish={handleChangePassword}
        >
          <Form.Item
            name="oldPassword"
            label="Current Password"
            rules={[{ required: true, message: 'Enter current password' }]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="New Password"
            rules={[
              { required: true, min: 6, message: 'At least 6 characters' },
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="Confirm New Password"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Confirm your new password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Passwords do not match'))
                },
              }),
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={pwSaving}>
              Change Password
            </Button>
            <Button
              style={{ marginLeft: 8 }}
              onClick={() => { setPwChanging(false); pwForm.resetFields() }}
            >
              Cancel
            </Button>
          </Form.Item>
        </Form>
      ) : (
        <Button icon={<LockOutlined />} onClick={() => setPwChanging(true)}>
          Change Password
        </Button>
      )}
    </Card>
  )
}

export default Profile
