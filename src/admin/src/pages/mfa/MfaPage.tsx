import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Tag,
  Typography,
  Spin,
  message,
  Modal,
  Result,
  Space,
} from 'antd'
import { ReloadOutlined, KeyOutlined } from '@ant-design/icons'
import type { MfaStatusVO } from '../../types'
import * as mfaApi from '../../api/mfa'

const { Title, Text, Paragraph } = Typography

const mfaLabels: Record<string, { label: string; color: string }> = {
  TOTP: { label: 'Authenticator App (TOTP)', color: 'blue' },
  SMS: { label: 'SMS', color: 'orange' },
  EMAIL: { label: 'Email', color: 'purple' },
  WEBAUTHN: { label: 'WebAuthn / Passkey', color: 'cyan' },
}

function MfaPage() {
  const [loading, setLoading] = useState(true)
  const [statuses, setStatuses] = useState<MfaStatusVO[]>([])
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null)
  const [codesOpen, setCodesOpen] = useState(false)
  const [error, setError] = useState(false)

  const fetchStatus = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const res = await mfaApi.getMfaStatus()
      setStatuses(res.data.data ?? [])
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchStatus() }, [fetchStatus])

  const handleGetBackupCodes = async () => {
    try {
      const res = await mfaApi.getBackupCodes()
      setBackupCodes(res.data.data ?? [])
      setCodesOpen(true)
    } catch {
      message.error('Failed to fetch backup codes')
    }
  }

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>MFA Status</Title>
        <Button icon={<ReloadOutlined />} onClick={fetchStatus}>Refresh</Button>
      </div>

      {error ? (
        <Result
          status="info"
          title="MFA Management"
          subTitle="MFA is managed per-user through the self-service portal. Administrators use MFA to secure their own accounts."
          extra={
            <Button type="primary" onClick={fetchStatus}>
              Check My MFA Status
            </Button>
          }
        />
      ) : statuses.length === 0 ? (
        <Result
          status="info"
          title="No MFA Configured"
          subTitle="You haven't set up any MFA methods yet. Use the API or self-service portal to enable MFA."
        />
      ) : (
        <>
          <Table
            dataSource={statuses}
            rowKey="mfaType"
            pagination={false}
            columns={[
              {
                title: 'Method',
                dataIndex: 'mfaType',
                key: 'mfaType',
                render: (v: string) => {
                  const info = mfaLabels[v] || { label: v, color: 'default' }
                  return <Tag color={info.color}>{info.label}</Tag>
                },
              },
              {
                title: 'Status',
                dataIndex: 'enabled',
                key: 'enabled',
                render: (v: boolean) =>
                  v ? <Tag color="green">Enabled</Tag> : <Tag color="default">Disabled</Tag>,
              },
            ]}
            style={{ maxWidth: 500 }}
          />

          <Space style={{ marginTop: 16 }}>
            <Button icon={<KeyOutlined />} onClick={handleGetBackupCodes}>
              View Backup Codes
            </Button>
          </Space>
        </>
      )}

      <Modal
        title="Backup Recovery Codes"
        open={codesOpen}
        onCancel={() => setCodesOpen(false)}
        footer={<Button onClick={() => setCodesOpen(false)}>Close</Button>}
      >
        <Paragraph type="warning">
          Store these codes in a safe place. Each code can only be used once.
        </Paragraph>
        {backupCodes && backupCodes.length > 0 ? (
          <div
            style={{
              background: '#f5f5f5',
              padding: 16,
              borderRadius: 4,
              fontFamily: 'monospace',
            }}
          >
            {backupCodes.map((code, i) => (
              <div key={i} style={{ padding: '2px 0' }}>
                {code}
              </div>
            ))}
          </div>
        ) : (
          <Text type="secondary">No backup codes available.</Text>
        )}
      </Modal>
    </Card>
  )
}

export default MfaPage
