import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Tabs, Button, Card, Form, Input, InputNumber, Modal, Space, Tag, message, Skeleton, Table, Popconfirm, Empty } from 'antd'
import { PlayCircleOutlined, StopOutlined, DeleteOutlined, ArrowLeftOutlined, SendOutlined, ReloadOutlined, CloudDownloadOutlined } from '@ant-design/icons'
import { listClusters, createCluster, deleteCluster, installCluster, startCluster, stopCluster, sendCommand, createBackup, type DstClusterInfo } from '../api/servers'

export default function ServerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const serverId = Number(id)
  const navigate = useNavigate()
  const [clusters, setClusters] = useState<DstClusterInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [consoleCmd, setConsoleCmd] = useState<Record<number, string>>({})
  const [consoleLog, setConsoleLog] = useState<string[]>([])
  const [form] = Form.useForm()
  const [activeTab, setActiveTab] = useState('clusters')

  const fetch = useCallback(async () => {
    setLoading(true)
    try { const res = await listClusters(serverId); if (res.code === 0) setClusters(res.data ?? []) }
    catch { message.error('Failed') } finally { setLoading(false) }
  }, [serverId])

  useEffect(() => { fetch() }, [fetch])

  const handleCreate = async (v: Record<string, unknown>) => {
    const res = await createCluster(serverId, v)
    if (res.code === 0) { message.success('Created'); setCreateOpen(false); form.resetFields(); fetch() }
  }

  const handleStart = async (cid: number) => {
    message.loading({ content: 'Installing...', key: 'deploy' })
    await installCluster(serverId, cid)
    const res = await startCluster(serverId, cid)
    message.success({ content: res.data?.success ? 'Started' : 'Failed: ' + (res.data?.output || ''), key: 'deploy' })
    fetch()
  }

  const handleStop = async (cid: number) => { await stopCluster(serverId, cid); message.success('Stopped'); fetch() }

  const handleCommand = async (cid: number) => {
    const cmd = consoleCmd[cid]; if (!cmd) return
    setConsoleLog((p) => [...p, '> ' + cmd])
    const res = await sendCommand(serverId, cid, cmd)
    setConsoleLog((p) => [...p, res.data?.success ? 'OK' : 'Failed'])
    setConsoleCmd((p) => ({ ...p, [cid]: '' }))
  }

  const handleBackup = async (cid: number) => {
    const res = await createBackup(serverId, cid)
    message.success(res.data?.success ? `Backup: ${res.data?.backupName} (${res.data?.size})` : 'Failed')
  }

  const statusColor: Record<string, string> = { running: 'green', stopped: 'default', error: 'red' }

  const clusterColumns = [
    { title: 'Name', dataIndex: 'displayName', render: (v: string, r: DstClusterInfo) => <Space><Tag color={statusColor[r.status]}>{r.status}</Tag>{v || r.name}</Space> },
    { title: 'Mode', dataIndex: 'gameMode', width: 100 },
    { title: 'Players', dataIndex: 'maxPlayers', width: 80 },
    { title: 'Created', dataIndex: 'createdAt', width: 170, render: (v: string) => v?.slice(0, 16) },
    { title: 'Actions', key: 'actions', width: 280, render: (_: unknown, r: DstClusterInfo) => (
        <Space>
          <Button size="small" icon={<PlayCircleOutlined />} type="primary" onClick={() => handleStart(r.id)}>Start</Button>
          <Button size="small" icon={<StopOutlined />} onClick={() => handleStop(r.id)}>Stop</Button>
          <Button size="small" icon={<CloudDownloadOutlined />} onClick={() => handleBackup(r.id)}>Backup</Button>
          <Popconfirm title="Delete?" onConfirm={() => { deleteCluster(serverId, r.id); fetch() }}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      )}
  ]

  const tabItems = [
    { key: 'clusters', label: 'Clusters', children: (
      <div>
        <div style={{ marginBottom: 16 }}><Button type="primary" onClick={() => setCreateOpen(true)}>Create Cluster</Button></div>
        <Table dataSource={clusters} columns={clusterColumns} rowKey="id" pagination={false} size="middle" />
        <Modal title="Create Cluster" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => form.submit()}>
          <Form form={form} layout="vertical" onFinish={handleCreate}>
            <Form.Item name="name" label="Directory Name" rules={[{ required: true }]}><Input placeholder="MyWorld" /></Form.Item>
            <Form.Item name="displayName" label="Display Name"><Input /></Form.Item>
            <Form.Item name="maxPlayers" label="Max Players" initialValue={6}><InputNumber min={1} max={64} /></Form.Item>
            <Form.Item name="clusterToken" label="Klei Token"><Input.TextArea rows={3} /></Form.Item>
          </Form>
        </Modal>
      </div>
    )},
    { key: 'console', label: 'Console', children: (
      <div>
        {clusters.map((c) => (
          <Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 16 }}>
            <Input.Search placeholder="c_save()" value={consoleCmd[c.id] || ''}
              onChange={(e) => setConsoleCmd((p) => ({ ...p, [c.id]: e.target.value }))}
              onSearch={() => handleCommand(c.id)} enterButton={<><SendOutlined /> Send</>} />
          </Card>
        ))}
        {consoleLog.length > 0 && (
          <Card size="small" title="Output" style={{ background: '#1e1e1e', color: '#d4d4d4', fontFamily: 'monospace', fontSize: 12 }}>
            {consoleLog.map((l, i) => <div key={i}>{l}</div>)}
          </Card>
        )}
      </div>
    )},
    { key: 'backups', label: 'Backups', children: (
      <div>
        {clusters.map((c) => (
          <Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 12 }}
            extra={<Button icon={<CloudDownloadOutlined />} onClick={() => handleBackup(c.id)}>Create Backup</Button>}>
            <Empty description="No backups yet — click Create Backup" />
          </Card>
        ))}
      </div>
    )},
    { key: 'mods', label: 'Mods', children: <Empty description="Mod management coming in Phase 2 UI" /> },
    { key: 'players', label: 'Players', children: <Empty description="Player list coming in Phase 2 UI" /> },
    { key: 'schedules', label: 'Schedules', children: <Empty description="Scheduled tasks coming in Phase 2 UI" /> },
  ]

  if (loading) return <Skeleton active />
  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/servers')}>Back</Button>
        <Button icon={<ReloadOutlined />} onClick={fetch}>Refresh</Button>
      </Space>
      <h2 style={{ marginBottom: 16 }}>Server #{serverId}</h2>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
    </div>
  )
}
