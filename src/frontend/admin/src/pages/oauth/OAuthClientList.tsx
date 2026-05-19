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
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { OAuthClientVO } from '../../types'
import * as oauthApi from '../../api/oauth'

const { Title } = Typography

function OAuthClientList() {
  const [clients, setClients] = useState<OAuthClientVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [secretOpen, setSecretOpen] = useState(false)
  const [secretData, setSecretData] = useState<{ clientId: string; clientSecret: string } | null>(null)
  const [selectedClient, setSelectedClient] = useState<OAuthClientVO | null>(null)
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()

  const fetchClients = useCallback(async () => {
    setLoading(true)
    try {
      const res = await oauthApi.listClients({ page, size })
      const d = res.data.data
      setClients(d?.records ?? [])
      setTotal(d?.total ?? 0)
    } catch {
      message.error('Failed to fetch OAuth clients')
    } finally {
      setLoading(false)
    }
  }, [page, size])

  useEffect(() => { fetchClients() }, [fetchClients])

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      await oauthApi.createClient(values as {
        clientId: string; clientName: string; clientType?: string;
        grantTypes?: string; redirectUris?: string; allowedScopes?: string; isTrusted?: number;
      })
      message.success('Client created')
      setCreateOpen(false)
      form.resetFields()
      fetchClients()
    } catch {
      message.error('Failed to create client')
    }
  }

  const handleEdit = async (values: Record<string, unknown>) => {
    if (!selectedClient) return
    try {
      await oauthApi.updateClient(selectedClient.id, values)
      message.success('Client updated')
      setEditOpen(false)
      editForm.resetFields()
      fetchClients()
    } catch {
      message.error('Failed to update client')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await oauthApi.deleteClient(id)
      message.success('Client deleted')
      fetchClients()
    } catch {
      message.error('Failed to delete client')
    }
  }

  const handleRegenerateSecret = async (id: number) => {
    try {
      const res = await oauthApi.regenerateSecret(id)
      setSecretData(res.data.data ?? null)
      setSecretOpen(true)
      fetchClients()
    } catch {
      message.error('Failed to regenerate secret')
    }
  }

  const columns: ColumnsType<OAuthClientVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true },
    { title: 'Name', dataIndex: 'clientName', key: 'clientName', ellipsis: true },
    { title: 'Type', dataIndex: 'clientType', key: 'clientType', width: 100, render: (v) => v || '-' },
    {
      title: 'Grant Types', dataIndex: 'grantTypes', key: 'grantTypes', width: 140,
      render: (v) => v ? v.split(',').map((t: string) => <Tag key={t}>{t.trim()}</Tag>) : '-',
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 90,
      render: (s: number) => (
        <Tag color={s === 1 ? 'green' : 'red'}>{s === 1 ? 'Active' : 'Disabled'}</Tag>
      ),
    },
    {
      title: 'Trusted', dataIndex: 'isTrusted', key: 'isTrusted', width: 80,
      render: (v: number) => v ? <Tag color="blue">Yes</Tag> : '-',
    },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 170, render: (v) => v || '-' },
    {
      title: 'Actions', key: 'actions', width: 280,
      render: (_: unknown, record: OAuthClientVO) => (
        <Space>
          <Button size="small" onClick={() => handleRegenerateSecret(record.id)}>
            Regenerate
          </Button>
          <Button size="small" onClick={() => {
            setSelectedClient(record)
            editForm.setFieldsValue(record)
            setEditOpen(true)
          }}>Edit</Button>
          <Popconfirm
            title="Delete this client?"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button size="small" danger>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>OAuth Clients</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchClients}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            New Client
          </Button>
        </Space>
      </div>

      <Table
        dataSource={clients}
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
        title="Create OAuth Client"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="clientId" label="Client ID" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="clientName" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="clientType" label="Type">
            <Input placeholder="e.g. public, confidential" />
          </Form.Item>
          <Form.Item name="grantTypes" label="Grant Types">
            <Input placeholder="e.g. authorization_code,client_credentials" />
          </Form.Item>
          <Form.Item name="redirectUris" label="Redirect URIs">
            <Input placeholder="e.g. https://app.example.com/callback" />
          </Form.Item>
          <Form.Item name="allowedScopes" label="Scopes">
            <Input placeholder="e.g. read,write" />
          </Form.Item>
          <Form.Item name="isTrusted" label="Trusted" valuePropName="checked">
            <Input type="checkbox" style={{ width: 20 }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title="Edit OAuth Client"
        open={editOpen}
        onCancel={() => { setEditOpen(false); editForm.resetFields() }}
        onOk={() => editForm.submit()}
      >
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item name="clientName" label="Name">
            <Input />
          </Form.Item>
          <Form.Item name="clientType" label="Type">
            <Input />
          </Form.Item>
          <Form.Item name="grantTypes" label="Grant Types">
            <Input />
          </Form.Item>
          <Form.Item name="redirectUris" label="Redirect URIs">
            <Input />
          </Form.Item>
          <Form.Item name="allowedScopes" label="Scopes">
            <Input />
          </Form.Item>
          <Form.Item name="isTrusted" label="Trusted" valuePropName="checked">
            <Input type="checkbox" style={{ width: 20 }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Secret Modal */}
      <Modal
        title="Client Secret"
        open={secretOpen}
        onCancel={() => setSecretOpen(false)}
        footer={<Button onClick={() => setSecretOpen(false)}>Close</Button>}
      >
        {secretData && (
          <div>
            <p><strong>Client ID:</strong> {secretData.clientId}</p>
            <p><strong>Client Secret:</strong> <code>{secretData.clientSecret}</code></p>
            <p style={{ color: '#faad14' }}>
              Copy this secret now. It will not be shown again.
            </p>
          </div>
        )}
      </Modal>
    </Card>
  )
}

export default OAuthClientList
