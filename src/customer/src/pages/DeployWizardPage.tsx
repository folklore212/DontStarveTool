import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Steps, Button, Form, Input, InputNumber, Select, Space, Tag, message, Result, Progress, Row, Col } from 'antd'
import { ArrowLeftOutlined, ArrowRightOutlined } from '@ant-design/icons'
import { listServers, createCluster, installCluster, startCluster, testConnection } from '../api/servers'

interface ServerItem { id: number; name: string; host: string; status: string }

export default function DeployWizardPage() {
  const [step, setStep] = useState(0)
  const [servers, setServers] = useState<ServerItem[]>([])
  const [selectedServer, setSelectedServer] = useState<number | null>(null)
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const [deploying, setDeploying] = useState(false)
  const [deployProgress, setDeployProgress] = useState(0)
  const [deployStep, setDeployStep] = useState('')
  const [deployLog, setDeployLog] = useState<string[]>([])
  const [deployResult, setDeployResult] = useState<'success' | 'failed' | null>(null)

  useEffect(() => {
    listServers().then((res) => {
      if (res.code === 0) setServers(res.data?.records as ServerItem[] ?? [])
    }).catch(() => {})
  }, [])

  const steps = [
    { title: 'Select Server', description: 'Choose where to deploy' },
    { title: 'Check Environment', description: 'Verify dependencies' },
    { title: 'Choose Template', description: 'Pick world preset' },
    { title: 'Configure World', description: 'Customize settings' },
    { title: 'Deploy', description: 'Launch the world' },
  ]

  const templates = [
    { key: 'survival', label: 'Newbie Survival', desc: 'Standard survival, 6 players', mem: '~1GB' },
    { key: 'pvp', label: 'PvP Arena', desc: 'Player vs Player, wilderness mode', mem: '~1.2GB' },
    { key: 'endless', label: 'Endless Mode', desc: 'Relaxed endless world', mem: '~1GB' },
    { key: 'caves', label: 'Cave Explorer', desc: 'Master + Caves, 8 players', mem: '~1.8GB' },
  ]

  const handleDeploy = async () => {
    if (!selectedServer) return
    setDeploying(true)
    setDeployProgress(0)
    setDeployLog([])

    const addLog = (msg: string) => setDeployLog((prev) => [...prev, msg])
    const updateProgress = (pct: number, msg: string) => {
      setDeployProgress(pct)
      setDeployStep(msg)
      addLog(msg)
    }

    try {
      updateProgress(10, 'Testing SSH connection...')
      const testRes = await testConnection(selectedServer)
      if (!testRes.data?.success) {
        setDeployResult('failed')
        addLog('SSH connection failed: ' + (testRes.data?.message || 'Unknown error'))
        setDeploying(false)
        return
      }
      addLog('SSH connection OK')

      const values = await form.validateFields()
      updateProgress(20, 'Creating cluster...')
      const createRes = await createCluster(selectedServer, {
        name: values.name || 'MyWorld',
        displayName: values.displayName || 'My World',
        gameMode: values.gameMode || 'survival',
        maxPlayers: values.maxPlayers || 6,
        clusterToken: values.token || '',
        masterPort: values.masterPort || 10999,
      })
      if (createRes.code !== 0) {
        setDeployResult('failed')
        addLog('Failed to create cluster: ' + createRes.message)
        setDeploying(false)
        return
      }
      const clusterId = createRes.data!.id
      addLog('Cluster created: ' + clusterId)

      updateProgress(40, 'Installing SteamCMD + DST...')
      const installRes = await installCluster(selectedServer, clusterId)
      addLog('Dependencies: ' + (installRes.data?.deps ? 'OK' : 'Failed'))
      addLog('SteamCMD: ' + (installRes.data?.steamCmd ? 'OK' : 'Failed'))
      addLog('DST: ' + (installRes.data?.dst ? 'OK' : 'Failed'))

      updateProgress(70, 'Generating world configuration...')
      addLog('cluster.ini + server.ini generated')

      updateProgress(85, 'Starting DST server...')
      const startRes = await startCluster(selectedServer, clusterId)
      if (startRes.data?.success) {
        updateProgress(100, 'World started successfully!')
        setDeployResult('success')
      } else {
        setDeployResult('failed')
        addLog('Start failed: ' + (startRes.data?.output || 'Unknown'))
      }
    } catch (e: unknown) {
      setDeployResult('failed')
      addLog('Error: ' + (e instanceof Error ? e.message : String(e)))
    } finally {
      setDeploying(false)
    }
  }

  const renderStepContent = () => {
    switch (step) {
      case 0: return (
        <div>
          <h3>Select a server to deploy to</h3>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            {servers.map((s) => (
              <Col xs={24} sm={12} key={s.id}>
                <Card hoverable onClick={() => setSelectedServer(s.id)}
                  style={{ border: selectedServer === s.id ? '2px solid #1677ff' : undefined }}>
                  <Space><Tag color={s.status === 'online' ? 'green' : 'red'}>{s.status}</Tag>{s.name}</Space>
                  <p style={{ color: '#888', marginTop: 8 }}>{s.host}</p>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      )
      case 1: return (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <h3>Environment Check</h3>
          <p>Click "Check" to verify dependencies on the selected server</p>
          {deployProgress > 0 ? (
            <Progress percent={deployProgress} status={deployResult === 'failed' ? 'exception' : 'active'} />
          ) : null}
          {deployLog.length > 0 && (
            <div style={{ background: '#1e1e1e', color: '#d4d4d4', padding: 16, borderRadius: 8, fontFamily: 'monospace', fontSize: 12, textAlign: 'left', marginTop: 16, maxHeight: 200, overflow: 'auto' }}>
              {deployLog.map((l, i) => <div key={i}>{l}</div>)}
            </div>
          )}
        </div>
      )
      case 2: return (
        <div>
          <h3>Choose a World Template</h3>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            {templates.map((t) => (
              <Col xs={24} sm={12} key={t.key}>
                <Card hoverable onClick={() => form.setFieldsValue({ gameMode: t.key })}
                  style={{ border: form.getFieldValue('gameMode') === t.key ? '2px solid #1677ff' : undefined }}>
                  <strong>{t.label}</strong>
                  <p style={{ color: '#888' }}>{t.desc}</p>
                  <Tag>{t.mem}</Tag>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      )
      case 3: return (
        <div style={{ maxWidth: 480, margin: '0 auto' }}>
          <h3>Configure Your World</h3>
          <Form form={form} layout="vertical" initialValues={{ name: 'MyWorld', displayName: 'My World', maxPlayers: 6, gameMode: 'survival', masterPort: 10999 }}>
            <Form.Item name="name" label="Directory Name" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="displayName" label="World Name"><Input /></Form.Item>
            <Form.Item name="gameMode" label="Game Mode"><Select options={[
              { value: 'survival', label: 'Survival' }, { value: 'endless', label: 'Endless' },
              { value: 'wilderness', label: 'Wilderness' },
            ]} /></Form.Item>
            <Form.Item name="maxPlayers" label="Max Players"><InputNumber min={1} max={64} /></Form.Item>
            <Form.Item name="masterPort" label="Port"><InputNumber /></Form.Item>
            <Form.Item name="token" label="Klei Server Token"><Input.TextArea rows={3} placeholder="Paste token from https://accounts.klei.com" /></Form.Item>
            <Form.Item name="password" label="Server Password (optional)"><Input.Password /></Form.Item>
          </Form>
        </div>
      )
      case 4: return (
        <div style={{ textAlign: 'center', padding: 40 }}>
          {deployResult === 'success' ? (
            <Result status="success" title="World Deployed!" subTitle="Your Don't Starve Together world is now running."
              extra={[<Button type="primary" key="goto" onClick={() => navigate(`/servers/${selectedServer}`)}>Manage Server</Button>]} />
          ) : deployResult === 'failed' ? (
            <Result status="error" title="Deployment Failed" subTitle="Check the logs below for details."
              extra={[<Button key="retry" onClick={() => { setDeployResult(null); setStep(0); setDeployProgress(0); setDeployLog([]) }}>Start Over</Button>]} />
          ) : (
            <div>
              <h3>Ready to Deploy</h3>
              <p>Click "Start Deployment" to begin</p>
              {deploying && <Progress percent={deployProgress} status="active" style={{ marginTop: 24 }} />}
              {deploying && <p style={{ marginTop: 8 }}>{deployStep}</p>}
              {deployLog.length > 0 && (
                <div style={{ background: '#1e1e1e', color: '#d4d4d4', padding: 16, borderRadius: 8, fontFamily: 'monospace', fontSize: 12, textAlign: 'left', marginTop: 16, maxHeight: 300, overflow: 'auto' }}>
                  {deployLog.map((l, i) => <div key={i}>{l}</div>)}
                </div>
              )}
            </div>
          )}
        </div>
      )
      default: return null
    }
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <h2 style={{ marginBottom: 24 }}>Deploy DST World</h2>
      <Steps current={step} items={steps} style={{ marginBottom: 32 }} />
      <Card>{renderStepContent()}</Card>
      <div style={{ marginTop: 24, display: 'flex', justifyContent: 'space-between' }}>
        <Button disabled={step === 0} onClick={() => setStep((s) => s - 1)} icon={<ArrowLeftOutlined />}>Previous</Button>
        {step < 4 ? (
          <Button type="primary" onClick={() => {
            if (step === 0 && !selectedServer) { message.warning('Please select a server'); return }
            if (step === 4) { handleDeploy(); return }
            setStep((s) => s + 1)
          }} icon={<ArrowRightOutlined />}>
            {step === 3 ? 'Start Deployment' : 'Next'}
          </Button>
        ) : (
          !deployResult && <Button type="primary" onClick={handleDeploy} loading={deploying}>{deploying ? 'Deploying...' : 'Start Deployment'}</Button>
        )}
      </div>
    </div>
  )
}
