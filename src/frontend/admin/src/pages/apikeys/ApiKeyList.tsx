import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Tag,
  Popconfirm,
  message,
  Typography,
  DatePicker,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ApiKeyVO } from '../../types'
import * as apikeyApi from '../../api/apikeys'

const { Title } = Typography

function ApiKeyList() {
  const [keys, setKeys] = useState<ApiKeyVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [createOpen, setCreateOpen] = useState(false)
  const [rawOpen, setRawOpen] = useState(false)
  const [rawKey, setRawKey] = useState<{ keyPrefix: string; rawKey: string; expiresAt: string | null } | null>(null)
  const [form] = Form.useForm()

  const fetchKeys = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apikeyApi.listApiKeys({ page, size })
      const d = res.data.data
      setKeys(d?.records ?? [])
      setTotal(d?.total ?? 0)
    } catch {
      message.error('Failed to fetch API keys')
    } finally {
      setLoading(false)
    }
  }, [page, size])

  useEffect(() => { fetchKeys() }, [fetchKeys])

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      const res = await apikeyApi.createApiKey({
        keyName: values.keyName as string,
        allowedScopes: values.allowedScopes as string | undefined,
        expiresAt: values.expiresAt ? (values.expiresAt as string) : undefined,
      })
      setRawKey(res.data.data ?? null)
      setCreateOpen(false)
      setRawOpen(true)
      form.resetFields()
      fetchKeys()
    } catch {
      message.error('Failed to create API key')
    }
  }

  const handleRevoke = async (keyId: number) => {
    try {
      await apikeyApi.revokeApiKey(keyId)
      message.success('API key revoked')
      fetchKeys()
    } catch {
      message.error('Failed to revoke API key')
    }
  }

  const handleRotate = async (keyId: number) => {
    try {
      const res = await apikeyApi.rotateApiKey(keyId)
      setRawKey(res.data.data ?? null)
      setRawOpen(true)
      fetchKeys()
    } catch {
      message.error('Failed to rotate API key')
    }
  }

  const columns: ColumnsType<ApiKeyVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'keyName', key: 'keyName' },
    { title: 'Prefix', dataIndex: 'keyPrefix', key: 'keyPrefix', render: (v) => <code>{v}</code> },
    { title: 'Scopes', dataIndex: 'allowedScopes', key: 'allowedScopes', ellipsis: true, render: (v) => v || '-' },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 90,
      render: (s: number) => (
        <Tag color={s === 1 ? 'green' : 'red'}>{s === 1 ? 'Active' : 'Revoked'}</Tag>
      ),
    },
    { title: 'Expires', dataIndex: 'expiresAt', key: 'expiresAt', width: 170, render: (v) => v || 'Never' },
    { title: 'Last Used', dataIndex: 'lastUsedAt', key: 'lastUsedAt', width: 170, render: (v) => v || 'Never' },
    {
      title: 'Actions', key: 'actions', width: 200,
      render: (_: unknown, record: ApiKeyVO) => (
        <Space>
          {record.status === 1 && (
            <>
              <Button size="small" onClick={() => handleRotate(record.id)}>Rotate</Button>
              <Popconfirm
                title="Revoke this API key?"
                onConfirm={() => handleRevoke(record.id)}
              >
                <Button size="small" danger>Revoke</Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>API Keys</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchKeys}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            New API Key
          </Button>
        </Space>
      </div>

      <Table
        dataSource={keys}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `Total ${t}`,
          onChange: (p, s) => { setPage(p); setSize(s) },
        }}
      />

      {/* Create Modal */}
      <Modal
        title="Create API Key"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="keyName" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="allowedScopes" label="Scopes">
            <Input placeholder="e.g. read,write" />
          </Form.Item>
          <Form.Item name="expiresAt" label="Expires At">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Raw Key Modal */}
      <Modal
        title="API Key Created"
        open={rawOpen}
        onCancel={() => setRawOpen(false)}
        footer={<Button onClick={() => setRawOpen(false)}>Close</Button>}
      >
        {rawKey && (
          <div>
            <p><strong>Key Prefix:</strong> {rawKey.keyPrefix}</p>
            <p><strong>Raw Key:</strong> <code>{rawKey.rawKey}</code></p>
            <p style={{ color: '#faad14' }}>
              Copy this key now. It will not be shown again.
            </p>
          </div>
        )}
      </Modal>
    </Card>
  )
}

export default ApiKeyList
