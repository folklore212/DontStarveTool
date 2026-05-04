import { useEffect, useState, useCallback } from 'react'
import { Card, Row, Col, Statistic, Progress, Skeleton, Empty, Tag } from 'antd'
import { CloudServerOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { listServers } from '../api/servers'

interface ServerStat { id: number; name: string; status: string; osInfo: string }

export default function AnalyticsPage() {
  const [servers, setServers] = useState<ServerStat[]>([])
  const [loading, setLoading] = useState(true)

  const fetch = useCallback(async () => {
    setLoading(true)
    try { const res = await listServers(1, 100); if (res.code === 0) setServers((res.data?.records ?? []) as ServerStat[]) }
    catch {} finally { setLoading(false) }
  }, [])

  useEffect(() => { fetch() }, [fetch])

  const total = servers.length
  const online = servers.filter((s) => s.status === 'online').length
  const offline = total - online
  const onlineRate = total > 0 ? Math.round((online / total) * 100) : 0

  if (loading) return <Skeleton active paragraph={{ rows: 6 }} />

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Analytics Dashboard</h2>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Total Servers" value={total} prefix={<CloudServerOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Online" value={online} suffix={`/ ${total}`} valueStyle={{ color: '#3f8600' }} prefix={<CloudServerOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Offline" value={offline} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Uptime Rate" value={onlineRate} suffix="%" prefix={<ThunderboltOutlined />} /></Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Server Status Distribution">
            <Progress percent={onlineRate} status={onlineRate > 50 ? 'active' : 'exception'} format={() => `${onlineRate}% Online`} />
            {servers.slice(0, 10).map((s) => (
              <div key={s.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px solid #f0f0f0' }}>
                <span>{s.name}</span>
                <Tag color={s.status === 'online' ? 'green' : 'red'}>{s.status}</Tag>
              </div>
            ))}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Quick Insights">
            {servers.length === 0 ? <Empty description="No servers yet — add one to see insights" /> : (
              <div>
                {online === 0 && <p style={{ color: '#faad14' }}>⚠ No servers are currently online</p>}
                {onlineRate < 50 && online > 0 && <p style={{ color: '#faad14' }}>⚠ Less than 50% servers online</p>}
                {total > 0 && <p>✅ {total} server{total > 1 ? 's' : ''} configured</p>}
                {online > 0 && <p style={{ color: '#52c41a' }}>🟢 {online} server{online > 1 ? 's' : ''} running</p>}
                <p>📊 Total server capacity available for DST world deployment</p>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
