import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Card, Form, Input, InputNumber, Modal, Space, Tag, message, Skeleton, Row, Col } from 'antd'
import { PlayCircleOutlined, StopOutlined, DeleteOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import { listClusters, createCluster, deleteCluster, installCluster, startCluster, stopCluster, sendCommand, type DstClusterInfo } from '../api/servers'

export default function ServerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const serverId = Number(id)
  const navigate = useNavigate()
  const [clusters, setClusters] = useState<DstClusterInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [consoleCmd, setConsoleCmd] = useState<Record<number, string>>({})
  const [form] = Form.useForm()

  const fetch = useCallback(async () => {
    setLoading(true)
    try { const res = await listClusters(serverId); if (res.code === 0) setClusters(res.data ?? []) }
    catch { message.error('Failed') }
    finally { setLoading(false) }
  }, [serverId])

  useEffect(() => { fetch() }, [fetch])

  const handleCreate = async (v: Record<string, unknown>) => {
    const res = await createCluster(serverId, v)
    if (res.code === 0) { message.success('Cluster created'); setCreateOpen(false); form.resetFields(); fetch() }
  }

  const handleStart = async (cid: number) => {
    await installCluster(serverId, cid)
    const res = await startCluster(serverId, cid)
    message.success(res.data?.success ? 'Started' : 'Failed')
    fetch()
  }

  const handleStop = async (cid: number) => {
    await stopCluster(serverId, cid)
    message.success('Stopped')
    fetch()
  }

  const handleCommand = async (cid: number) => {
    const cmd = consoleCmd[cid]
    if (!cmd) return
    await sendCommand(serverId, cid, cmd)
    message.success('Command sent')
    setConsoleCmd((p) => ({ ...p, [cid]: '' }))
  }

  const statusColor: Record<string, string> = { running: 'green', stopped: 'default', error: 'red' }

  if (loading) return <Skeleton active />
  return (
    <div>
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/servers')} style={{ marginBottom: 16 }}>Back</Button>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>Clusters</h2>
        <Button type="primary" onClick={() => setCreateOpen(true)}>Create Cluster</Button>
      </div>
      <Row gutter={[16, 16]}>
        {clusters.map((c) => (
          <Col xs={24} lg={12} key={c.id}>
            <Card title={<Space><Tag color={statusColor[c.status]}>{c.status}</Tag>{c.displayName || c.name}</Space>}
              extra={<Button size="small" danger icon={<DeleteOutlined />} onClick={() => { deleteCluster(serverId, c.id); fetch() }} />}>
              <p>Mode: {c.gameMode} | Max: {c.maxPlayers}</p>
              <Space style={{ marginTop: 8 }}>
                <Button icon={<PlayCircleOutlined />} type="primary" onClick={() => handleStart(c.id)}>Start</Button>
                <Button icon={<StopOutlined />} onClick={() => handleStop(c.id)}>Stop</Button>
              </Space>
              <div style={{ marginTop: 12 }}>
                <Input.Search placeholder="c_save()" value={consoleCmd[c.id] || ''}
                  onChange={(e) => setConsoleCmd((p) => ({ ...p, [c.id]: e.target.value }))}
                  onSearch={() => handleCommand(c.id)} enterButton="Send" />
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Modal title="Create Cluster" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => form.submit()}>
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Directory Name" rules={[{ required: true }]}><Input placeholder="MyWorld" /></Form.Item>
          <Form.Item name="displayName" label="Display Name"><Input placeholder="My World" /></Form.Item>
          <Form.Item name="gameMode" label="Game Mode" initialValue="survival">
            <Input />
          </Form.Item>
          <Form.Item name="maxPlayers" label="Max Players" initialValue={6}><InputNumber min={1} max={64} /></Form.Item>
          <Form.Item name="masterPort" label="Master Port" initialValue={10999}><InputNumber /></Form.Item>
          <Form.Item name="clusterToken" label="Klei Server Token"><Input.TextArea rows={3} placeholder="Paste your Klei token here" /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
