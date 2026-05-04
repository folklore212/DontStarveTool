import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Tabs, Button, Card, Form, Input, InputNumber, Modal, Space, Tag, message, Skeleton, Table, Popconfirm, Empty, List } from 'antd'
import { PlayCircleOutlined, StopOutlined, DeleteOutlined, ArrowLeftOutlined, SendOutlined, ReloadOutlined, CloudDownloadOutlined, SearchOutlined, UserDeleteOutlined, DownloadOutlined } from '@ant-design/icons'
import { listClusters, createCluster, deleteCluster, installCluster, startCluster, stopCluster, sendCommand, createBackup, type DstClusterInfo } from '../api/servers'
import client from '../api/client'

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
  const [players, setPlayers] = useState<{ clusterId: number; output: string }[]>([])
  const [modSearch, setModSearch] = useState('')
  const [modResults, setModResults] = useState<Record<string, unknown>[]>([])
  const [modInstalling, setModInstalling] = useState(false)
  const [backups, setBackups] = useState<Record<number, string[]>>({})

  const fetch = useCallback(async () => {
    setLoading(true)
    try { const res = await listClusters(serverId); if (res.code === 0) setClusters(res.data ?? []) }
    catch { message.error('Failed') } finally { setLoading(false) }
  }, [serverId])
  useEffect(() => { fetch() }, [fetch])

  const handleStart = async (cid: number) => {
    message.loading({ content: 'Installing...', key: 'd' })
    await installCluster(serverId, cid)
    const r = await startCluster(serverId, cid)
    message.success({ content: r.data?.success ? 'Started' : 'Failed', key: 'd' })
    fetch()
  }
  const handleStop = async (cid: number) => { await stopCluster(serverId, cid); message.success('Stopped'); fetch() }
  const handleBackup = async (cid: number) => {
    const r = await createBackup(serverId, cid)
    message.success(r.data?.success ? 'Done' : 'Failed')
    setBackups((p) => ({ ...p, [cid]: [...(p[cid] || []), r.data?.backupName || ''] }))
  }
  const handleCommand = async (cid: number) => {
    const cmd = consoleCmd[cid]; if (!cmd) return
    setConsoleLog((p) => [...p, '> ' + cmd])
    const r = await sendCommand(serverId, cid, cmd)
    setConsoleLog((p) => [...p, r.data?.success ? 'OK' : 'Failed'])
    setConsoleCmd((p) => ({ ...p, [cid]: '' }))
  }
  const fetchPlayers = async (cid: number) => {
    try {
      const r = await client.get(`/servers/${serverId}/clusters/${cid}/players`)
      setPlayers((p) => [...p.filter((x) => x.clusterId !== cid), { clusterId: cid, output: (r.data as any)?.data?.output || 'No players' }])
    } catch { message.error('Failed') }
  }
  const kickPlayer = async (cid: number, steamId: string) => {
    await client.post(`/servers/${serverId}/clusters/${cid}/players/kick`, { steamId })
    message.success('Kicked'); fetchPlayers(cid)
  }
  const searchMods = async () => {
    if (!modSearch || !clusters[0]) return
    try {
      const r = await client.post(`/servers/${serverId}/clusters/${clusters[0].id}/mods/search`, { keyword: modSearch })
      setModResults(((r.data as any)?.data?.results as Record<string, unknown>[]) || [])
    } catch { message.error('Search failed') }
  }
  const installMod = async (wid: string) => {
    if (!clusters[0]) return; setModInstalling(true)
    await client.post(`/servers/${serverId}/clusters/${clusters[0].id}/mods/install`, { workshopId: wid })
    message.success('Installed ' + wid); setModInstalling(false)
  }
  const handleCreate = async (v: Record<string, unknown>) => {
    const r = await createCluster(serverId, v)
    if (r.code === 0) { message.success('Created'); setCreateOpen(false); form.resetFields(); fetch() }
  }

  const sc: Record<string, string> = { running: 'green', stopped: 'default', error: 'red' }
  const cols = [
    { title: 'Name', dataIndex: 'displayName', render: (v: string, r: DstClusterInfo) => <Space><Tag color={sc[r.status]}>{r.status}</Tag>{v || r.name}</Space> },
    { title: 'Mode', dataIndex: 'gameMode', width: 100 },
    { title: 'Players', dataIndex: 'maxPlayers', width: 80 },
    { title: 'Actions', key: 'a', width: 280, render: (_: unknown, r: DstClusterInfo) => (
        <Space>
          <Button size="small" icon={<PlayCircleOutlined />} type="primary" onClick={() => handleStart(r.id)}>Start</Button>
          <Button size="small" icon={<StopOutlined />} onClick={() => handleStop(r.id)}>Stop</Button>
          <Button size="small" icon={<CloudDownloadOutlined />} onClick={() => handleBackup(r.id)}>Backup</Button>
          <Popconfirm title="Delete?" onConfirm={() => { deleteCluster(serverId, r.id); fetch() }}>
            <Button size="small" danger icon={<DeleteOutlined />} /></Popconfirm>
        </Space>)}
  ]

  const tabs = [
    { key: 'clusters', label: 'Clusters', children: (<div>
      <div style={{ marginBottom: 16 }}><Button type="primary" onClick={() => setCreateOpen(true)}>Create Cluster</Button></div>
      <Table dataSource={clusters} columns={cols} rowKey="id" pagination={false} size="middle" />
      <Modal title="Create Cluster" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={() => form.submit()}>
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Directory Name" rules={[{ required: true }]}><Input placeholder="MyWorld" /></Form.Item>
          <Form.Item name="displayName" label="Display Name"><Input /></Form.Item>
          <Form.Item name="maxPlayers" label="Max Players" initialValue={6}><InputNumber min={1} max={64} /></Form.Item>
          <Form.Item name="clusterToken" label="Klei Token"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>)},
    { key: 'console', label: 'Console', children: (<div>
      {clusters.map((c) => (<Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 16 }}>
        <Input.Search placeholder="c_save()" value={consoleCmd[c.id] || ''}
          onChange={(e) => setConsoleCmd((p) => ({ ...p, [c.id]: e.target.value }))}
          onSearch={() => handleCommand(c.id)} enterButton={<><SendOutlined /> Send</>} /></Card>))}
      {consoleLog.length > 0 && (<Card size="small" title="Output" style={{ background: '#1e1e1e', color: '#d4d4d4', fontFamily: 'monospace', fontSize: 12 }}>
        {consoleLog.map((l, i) => <div key={i}>{l}</div>)}</Card>)}
    </div>)},
    { key: 'players', label: 'Players', children: (<div>{clusters.map((c) => (<Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 12 }}
      extra={<Button size="small" icon={<ReloadOutlined />} onClick={() => fetchPlayers(c.id)}>Refresh</Button>}>
      {players.find((p) => p.clusterId === c.id) ? <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>{players.find((p) => p.clusterId === c.id)!.output}</pre> : <Empty description="Click Refresh to load" />}
      <Space style={{ marginTop: 8 }}><Input placeholder="Steam ID" style={{ width: 200 }} id={`kick-${c.id}`} />
        <Button danger icon={<UserDeleteOutlined />} onClick={() => { const el = document.getElementById(`kick-${c.id}`) as HTMLInputElement; if (el?.value) kickPlayer(c.id, el.value) }}>Kick</Button></Space>
    </Card>))}</div>)},
    { key: 'mods', label: 'Mods', children: (<div>
      <Space style={{ marginBottom: 16 }}><Input.Search placeholder="Search Workshop..." value={modSearch} onChange={(e) => setModSearch(e.target.value)} onSearch={searchMods} enterButton={<><SearchOutlined /> Search</>} style={{ width: 300 }} /></Space>
      {modResults.length > 0 && <List dataSource={modResults} renderItem={(m: Record<string, unknown>) => (<List.Item actions={[<Button key="i" size="small" type="primary" loading={modInstalling} onClick={() => installMod(String(m.workshopId))}>Install</Button>]}><div><strong>{String(m.title || 'Unknown')}</strong><div style={{ color: '#888', fontSize: 12 }}>ID: {String(m.workshopId || '')} · Subs: {String(m.subscriptions || '0')}</div></div></List.Item>)} />}
    </div>)},
    { key: 'backups', label: 'Backups', children: (<div>{clusters.map((c) => (<Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 12 }}
      extra={<Button icon={<CloudDownloadOutlined />} onClick={() => handleBackup(c.id)}>Create Backup</Button>}>
      {backups[c.id]?.length ? <List size="small" dataSource={backups[c.id]} renderItem={(n: string) => (<List.Item actions={[<Button key="r" size="small" icon={<DownloadOutlined />}>Restore</Button>]}>{n}</List.Item>)} /> : <Empty description="No backups yet" />}
    </Card>))}</div>)},
    { key: 'schedules', label: 'Schedules', children: (<div>{clusters.map((c) => (<Card key={c.id} size="small" title={c.displayName || c.name} style={{ marginBottom: 12 }}
      extra={<Button size="small" type="primary">Add Schedule</Button>}><Empty description="No schedules — add auto backup, restart, or announcements" /></Card>))}</div>)},
  ]

  if (loading) return <Skeleton active />
  return (<div>
    <Space style={{ marginBottom: 16 }}><Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/servers')}>Back</Button><Button icon={<ReloadOutlined />} onClick={fetch}>Refresh</Button></Space>
    <h2 style={{ marginBottom: 16 }}>Server #{serverId}</h2>
    <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabs} />
  </div>)
}
