import { useEffect, useState, useCallback } from 'react'
import { Card, Row, Col, Tag, Space, Rate, Input, Select, message, Skeleton, Empty } from 'antd'
import { SearchOutlined, ForkOutlined, RocketOutlined } from '@ant-design/icons'
import { browseMarketplace, forkConfig, deployConfig, type MarketConfigInfo } from '../api/marketplace'
import { listServers } from '../api/servers'

export default function MarketplacePage() {
  const [configs, setConfigs] = useState<MarketConfigInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<string | undefined>()
  const [sort, setSort] = useState('downloads')
  const [servers, setServers] = useState<{ id: number; name: string }[]>([])

  const fetch = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { sort }
      if (category) params.category = category
      const res = await browseMarketplace(params)
      if (res.code === 0) setConfigs(res.data?.records ?? [])
    } catch { message.error('Failed to load marketplace') }
    finally { setLoading(false) }
  }, [category, sort])

  useEffect(() => { fetch(); listServers().then((r) => { if (r.code === 0) setServers((r.data?.records ?? []) as { id: number; name: string }[]) }).catch(() => {}) }, [fetch])

  const handleFork = async (id: number) => {
    const res = await forkConfig(id)
    if (res.code === 0) { message.success('Forked!'); fetch() }
  }

  const handleDeploy = async (configId: number) => {
    if (servers.length === 0) { message.warning('Add a server first'); return }
    // Deploy to first available server
    const res = await deployConfig(configId, servers[0].id)
    if (res.code === 0) message.success('Deployed! Check your servers')
  }

  const filtered = configs.filter((c) => !keyword || c.title.toLowerCase().includes(keyword.toLowerCase()) || (c.tags && c.tags.includes(keyword)))

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Config Marketplace</h2>
      <Space style={{ marginBottom: 24, width: '100%' }} wrap>
        <Input prefix={<SearchOutlined />} placeholder="Search configs..." value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 240 }} allowClear />
        <Select value={category} onChange={setCategory} allowClear placeholder="Category" style={{ width: 140 }}
          options={[{ value: 'survival', label: 'Survival' }, { value: 'pvp', label: 'PvP' }, { value: 'caves', label: 'Caves' }, { value: 'modpack', label: 'Modpack' }]} />
        <Select value={sort} onChange={setSort} style={{ width: 140 }}
          options={[{ value: 'downloads', label: 'Most Downloaded' }, { value: 'rating', label: 'Top Rated' }, { value: 'newest', label: 'Newest' }]} />
      </Space>

      {loading ? <Skeleton active paragraph={{ rows: 6 }} /> : filtered.length === 0 ? <Empty description="No configs found" /> : (
        <Row gutter={[16, 16]}>
          {filtered.map((c) => (
            <Col xs={24} sm={12} lg={8} key={c.id}>
              <Card hoverable title={c.title} extra={<Tag color={c.verified ? 'gold' : 'default'}>{c.verified ? 'Verified' : c.category}</Tag>}
                actions={[
                  <Space key="fork"><ForkOutlined /><span onClick={() => handleFork(c.id)}>Fork ({c.downloadCount || 0})</span></Space>,
                  <Space key="deploy"><RocketOutlined /><span onClick={() => handleDeploy(c.id)}>Deploy</span></Space>,
                ]}
              >
                <p style={{ color: '#888', height: 40, overflow: 'hidden' }}>{c.description || 'No description'}</p>
                <Space style={{ marginTop: 8 }}>
                  <Rate disabled value={Math.round(c.ratingAvg || 0)} style={{ fontSize: 14 }} />
                  <span style={{ color: '#888' }}>({c.ratingCount || 0})</span>
                </Space>
                <Tag style={{ marginTop: 8 }}>{c.gameMode || 'unknown'}</Tag>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}
