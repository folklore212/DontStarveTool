import { useEffect, useState, useCallback } from 'react'
import { Card, Button, Modal, Form, Input, InputNumber, Select, Tag, Space, message, Popconfirm, Row, Col, Skeleton } from 'antd'
import { PlusOutlined, ReloadOutlined, PlayCircleOutlined, DeleteOutlined } from '@ant-design/icons'
import { listServers, createServer, deleteServer, testConnection, type ServerInfo } from '../api/servers'
import { useNavigate } from 'react-router-dom'

export default function ServersPage() {
  const [servers, setServers] = useState<ServerInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [testing, setTesting] = useState<Record<number, boolean>>({})
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const fetchServers = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listServers()
      if (res.code === 0) setServers(res.data?.records ?? [])
    } catch { message.error('Failed to load servers') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchServers() }, [fetchServers])

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      const res = await createServer(values as Partial<ServerInfo>)
      if (res.code === 0) { message.success('Server added'); setModalOpen(false); form.resetFields(); fetchServers() }
      else message.error(res.message)
    } catch { message.error('Failed to add server') }
  }

  const handleTest = async (id: number) => {
    setTesting((p) => ({ ...p, [id]: true }))
    try {
      const res = await testConnection(id)
      if (res.code === 0) {
        message.success(res.data?.success ? 'Connection OK' : `Failed: ${res.data?.message}`)
        fetchServers()
      }
    } catch { message.error('Test failed') }
    finally { setTesting((p) => ({ ...p, [id]: false })) }
  }

  const handleDelete = async (id: number) => {
    await deleteServer(id)
    message.success('Deleted')
    fetchServers()
  }

  const statusColor: Record<string, string> = { online: 'green', offline: 'red', unknown: 'default' }

  if (loading) return <Skeleton active paragraph={{ rows: 6 }} />

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h2>Servers</h2>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchServers}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>Add Server</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        {servers.map((s) => (
          <Col xs={24} sm={12} lg={8} key={s.id}>
            <Card
              hoverable
              onClick={() => navigate(`/servers/${s.id}`)}
              title={<Space><Tag color={statusColor[s.status]}>{s.status}</Tag>{s.name}</Space>}
              extra={
                <Space onClick={(e) => e.stopPropagation()}>
                  <Button size="small" icon={<ReloadOutlined />} loading={testing[s.id]} onClick={() => handleTest(s.id)} />
                  <Popconfirm title="Delete?" onConfirm={() => handleDelete(s.id)}><Button size="small" danger icon={<DeleteOutlined />} /></Popconfirm>
                </Space>
              }
            >
              <p>{s.host}:{s.port} @ {s.username}</p>
              {s.osInfo && <p style={{ color: '#888', fontSize: 12 }}>{s.osInfo.slice(0, 80)}</p>}
              <Space style={{ marginTop: 8 }}>
                <Button size="small" icon={<PlayCircleOutlined />} onClick={(e) => { e.stopPropagation(); navigate(`/servers/${s.id}`) }}>Manage</Button>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Modal title="Add Server" open={modalOpen} onCancel={() => setModalOpen(false)} onOk={() => form.submit()}>
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input placeholder="My Server" /></Form.Item>
          <Form.Item name="host" label="Host" rules={[{ required: true }]}><Input placeholder="192.168.1.100" /></Form.Item>
          <Form.Item name="port" label="SSH Port" initialValue={22}><InputNumber min={1} max={65535} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="username" label="Username" initialValue="root"><Input /></Form.Item>
          <Form.Item name="authType" label="Auth Type" initialValue="password">
            <Select options={[{ value: 'password', label: 'Password' }, { value: 'key', label: 'Private Key' }]} />
          </Form.Item>
          <Form.Item name="password" label="Password / Key"><Input.Password /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
