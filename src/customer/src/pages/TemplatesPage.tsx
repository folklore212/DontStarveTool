import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card, Row, Col, Tag, Space, Tabs, Input, Select, message, Skeleton,
  Empty, Badge, Image, Button, Modal, Descriptions, Rate, Tooltip, Divider, Popconfirm
} from 'antd'
import {
  SearchOutlined, ForkOutlined, RocketOutlined, FireOutlined,
  DownloadOutlined, StarOutlined, EyeOutlined, ExperimentOutlined,
  GlobalOutlined, BranchesOutlined,
  FieldTimeOutlined, SunOutlined, PartitionOutlined, SettingOutlined,
  EditOutlined, DeleteOutlined, PlusOutlined,
  BugOutlined, GiftOutlined, CloudOutlined, SyncOutlined, ToolOutlined
} from '@ant-design/icons'
import {
  browseTemplates, forkTemplate, getTemplateDetail,
  browseWorldGenPresets, getHotMods,
  searchWorkshopMods,
  deleteTemplate, deleteWorldGenPreset,
  type TemplateInfo, type WorldGenPresetInfo,
  type WorkshopModInfo, type TemplateFullDetail
} from '../api/templates'
import { useTranslation } from '../i18n'
import { useAuth } from '../context/AuthContext'
import TemplateFormModal from '../components/TemplateFormModal'
import WorldGenPresetFormModal from '../components/WorldGenPresetFormModal'

const SVG_PLACEHOLDER_TEMPLATE = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjYmZiZmJmIiBmb250LXNpemU9IjI0Ij5UZW1wbGF0ZTwvdGV4dD48L3N2Zz4='
const SVG_PLACEHOLDER_WORLDGEN = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjMWExYTJlIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjZmZmIiBmb250LXNpemU9IjI4Ij7wn4yN88K/PC90ZXh0Pjwvc3ZnPg=='
const SVG_PLACEHOLDER_MOD = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjYmZiZmJmIiBmb250LXNpemU9IjI4Ij5Nb2Q8L3RleHQ+PC9zdmc+'

const PRESET_COLORS = [
  '#1677ff', '#52c41a', '#fa8c16', '#13c2c2',
  '#722ed1', '#2f54eb', '#f5222d', '#faad14',
  '#434343', '#eb2f96', '#a0d911', '#fa541c',
]

function presetGradient(index: number) {
  const c = PRESET_COLORS[index % PRESET_COLORS.length]
  return `linear-gradient(135deg, ${c} 0%, ${c}88 100%)`
}

const settingIcons: Record<string, React.ReactNode> = {
  worldSize: <GlobalOutlined />,
  branching: <BranchesOutlined />,
  loopMode: <PartitionOutlined />,
  seasonStart: <FieldTimeOutlined />,
  dayMode: <SunOutlined />,
  autumnLength: <FieldTimeOutlined />,
  winterLength: <FieldTimeOutlined />,
  springLength: <FieldTimeOutlined />,
  summerLength: <FieldTimeOutlined />,
  resourceVariety: <ExperimentOutlined />,
}

function SettingBadge({ setting, value }: { setting: string; value: string }) {
  const { t } = useTranslation()
  const labelKey = `templates_${setting.replace(/([A-Z])/g, '_$1').toLowerCase()}`
  const label = t(`common.${labelKey}`)
  return (
    <Tooltip title={`${label !== `common.${labelKey}` ? label : setting}: ${value}`}>
      <Tag icon={settingIcons[setting]} color="blue" style={{ marginBottom: 4 }}>
        {value}
      </Tag>
    </Tooltip>
  )
}

export default function TemplatesPage() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const { state } = useAuth()
  const currentUserId = state.userInfo?.userId
  const tVal = (v: string | undefined) => {
    if (!v || v === 'default') return t('common.templates_val_default')
    const key = `templates_val_${v.replace(/-/g, '')}`
    const translated = t(`common.${key}`)
    return translated !== `common.${key}` ? translated : v
  }
  const [activeTab, setActiveTab] = useState('server')
  const [loading, setLoading] = useState(true)
  const [templates, setTemplates] = useState<TemplateInfo[]>([])
  const [presets, setPresets] = useState<WorldGenPresetInfo[]>([])
  const [workshopMods, setWorkshopMods] = useState<WorkshopModInfo[]>([])
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<string | undefined>()
  const [sort, setSort] = useState('downloads')

  const [detailOpen, setDetailOpen] = useState(false)
  const [detailData, setDetailData] = useState<TemplateFullDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [presetOpen, setPresetOpen] = useState(false)
  const [selectedPreset, setSelectedPreset] = useState<WorldGenPresetInfo | null>(null)

  const [templateFormOpen, setTemplateFormOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<TemplateInfo | null>(null)
  const [presetFormOpen, setPresetFormOpen] = useState(false)
  const [editingPreset, setEditingPreset] = useState<WorldGenPresetInfo | null>(null)

  const fetchTemplates = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { sort, type: 'server_template' }
      if (category) params.category = category
      const res = await browseTemplates(params)
      if (res.code === 0) setTemplates(res.data?.records ?? [])
    } catch { message.error(t('common.templates_load_failed')) }
    finally { setLoading(false) }
  }, [category, sort, t])

  const fetchPresets = useCallback(async () => {
    try {
      const presetsRes = await browseWorldGenPresets()
      if (presetsRes.code === 0) setPresets(presetsRes.data?.records ?? [])
    } catch { message.error(t('common.templates_presets_load_failed')) }
  }, [t])

  const fetchWorkshop = useCallback(async () => {
    try {
      const res = keyword
        ? await searchWorkshopMods(keyword)
        : await getHotMods()
      if (res.code === 0) setWorkshopMods(res.data ?? [])
    } catch { message.error(t('common.templates_workshop_load_failed')) }
  }, [keyword, t])

  useEffect(() => { fetchTemplates() }, [activeTab, category, sort])
  useEffect(() => { if (activeTab === 'presets') fetchPresets() }, [activeTab])
  useEffect(() => { if (activeTab === 'workshop') fetchWorkshop() }, [activeTab, keyword])

  const handleViewDetail = async (id: number) => {
    setDetailLoading(true)
    try {
      const res = await getTemplateDetail(id)
      if (res.code === 0) {
        setDetailData(res.data ?? null)
        setDetailOpen(true)
      }
    } catch { message.error(t('common.templates_detail_failed')) }
    finally { setDetailLoading(false) }
  }

  const handleFork = async (id: number) => {
    try {
      const res = await forkTemplate(id)
      if (res.code === 0) { message.success(t('common.templates_fork_success')); fetchTemplates() }
      else message.error(t('common.templates_fork_failed'))
    } catch { message.error(t('common.templates_fork_failed')) }
  }

  const handleDeploy = (templateId: number) => {
    navigate('/servers/deploy', { state: { templateId } })
  }

  const handleCreateTemplate = () => {
    setEditingTemplate(null)
    setTemplateFormOpen(true)
  }

  const handleEditTemplate = (t: TemplateInfo) => {
    setEditingTemplate(t)
    setTemplateFormOpen(true)
  }

  const handleDeleteTemplate = async (id: number) => {
    try {
      const res = await deleteTemplate(id)
      if (res.code === 0) { message.success('Deleted'); fetchTemplates() }
      else message.error(res.message)
    } catch { message.error('Delete failed') }
  }

  const handleCreatePreset = () => {
    setEditingPreset(null)
    setPresetFormOpen(true)
  }

  const handleEditPreset = (p: WorldGenPresetInfo) => {
    setEditingPreset(p)
    setPresetFormOpen(true)
  }

  const handleDeletePreset = async (id: number) => {
    try {
      const res = await deleteWorldGenPreset(id)
      if (res.code === 0) { message.success('Deleted'); fetchPresets() }
      else message.error(res.message)
    } catch { message.error('Delete failed') }
  }

  const filteredTemplates = templates.filter((t) =>
    !keyword || t.name.toLowerCase().includes(keyword.toLowerCase()) ||
    (t.description && t.description.toLowerCase().includes(keyword.toLowerCase()))
  )

  const filteredPresets = presets.filter((p) =>
    !keyword || p.name.toLowerCase().includes(keyword.toLowerCase()) ||
    (p.description && p.description.toLowerCase().includes(keyword.toLowerCase()))
  )

  const renderServerTemplates = () => (
    <>
      <Space style={{ marginBottom: 24, width: '100%' }} wrap>
        <Input
          prefix={<SearchOutlined />}
          placeholder={t('common.templates_search')}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ width: 240 }}
          allowClear
        />
        <Select
          value={category}
          onChange={setCategory}
          allowClear
          placeholder={t('common.templates_category')}
          style={{ width: 140 }}
          options={[
            { value: 'survival', label: 'Survival' },
            { value: 'pvp', label: 'PvP' },
            { value: 'caves', label: 'Caves' },
            { value: 'modpack', label: 'Modpack' },
            { value: 'endless', label: 'Endless' },
          ]}
        />
        <Select
          value={sort}
          onChange={setSort}
          style={{ width: 160 }}
          options={[
            { value: 'downloads', label: t('common.templates_downloads') },
            { value: 'rating', label: t('common.templates_top_rated') },
            { value: 'newest', label: t('common.templates_newest') },
          ]}
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateTemplate}>
          {t('common.templates_create_template')}
        </Button>
      </Space>
      {loading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : filteredTemplates.length === 0 ? (
        <Empty description={t('common.templates_no_results')} />
      ) : (
        <Row gutter={[16, 16]}>
          {filteredTemplates.map((t2) => (
            <Col xs={24} sm={12} lg={8} xl={6} key={t2.id}>
              <Card
                hoverable
                cover={
                  t2.coverImage ? (
                    <div style={{ height: 160, overflow: 'hidden', background: '#f0f0f0' }}>
                      <Image
                        src={t2.coverImage} alt={t2.name}
                        style={{ objectFit: 'cover', width: '100%', height: '100%' }}
                        fallback={SVG_PLACEHOLDER_TEMPLATE}
                      />
                    </div>
                  ) : (
                    <div style={{ height: 160, background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <SettingOutlined style={{ fontSize: 48, color: 'rgba(255,255,255,0.6)' }} />
                    </div>
                  )
                }
                actions={[
                  <Tooltip title={t('common.templates_view_detail')} key="view">
                    <EyeOutlined onClick={() => handleViewDetail(t2.id)} />
                  </Tooltip>,
                  <Tooltip title={`${t('common.templates_fork')} (${t2.downloadCount || 0})`} key="fork">
                    <Space><ForkOutlined onClick={() => handleFork(t2.id)} /><span>{t2.downloadCount || 0}</span></Space>
                  </Tooltip>,
                  <Tooltip title={t('common.templates_deploy')} key="deploy">
                    <RocketOutlined onClick={() => handleDeploy(t2.id)} />
                  </Tooltip>,
                  ...(currentUserId && currentUserId === t2.authorId ? [
                    <Tooltip title={t('common.edit')} key="edit">
                      <EditOutlined onClick={() => handleEditTemplate(t2)} />
                    </Tooltip>,
                    <Popconfirm key="del" title={t('common.templates_delete_confirm')} onConfirm={() => handleDeleteTemplate(t2.id)}>
                      <DeleteOutlined />
                    </Popconfirm>,
                  ] : []),
                ]}
              >
                <Card.Meta
                  title={
                    <Space>
                      {t2.name}
                      {t2.verified ? <Tag color="gold" style={{ fontSize: 10 }}>{t('common.templates_verified')}</Tag> : null}
                    </Space>
                  }
                  description={
                    <>
                      <p style={{ color: '#888', height: 36, overflow: 'hidden', fontSize: 13, marginBottom: 8 }}>
                        {t2.description || t('common.templates_desc_default')}
                      </p>
                      <Space wrap size={[0, 4]}>
                        <Tag color="blue">{t2.category || t('common.templates_category_general')}</Tag>
                        <Tag>{t2.gameMode || 'survival'}</Tag>
                        <Tag>{t2.maxPlayers || 6}p</Tag>
                      </Space>
                      <div style={{ marginTop: 8 }}>
                        <Rate disabled value={Math.round(t2.ratingAvg || 0)} style={{ fontSize: 12 }} />
                        <span style={{ color: '#888', fontSize: 12, marginLeft: 4 }}>
                          ({t2.ratingCount || 0})
                        </span>
                      </div>
                    </>
                  }
                />
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </>
  )

  const renderWorldGenPresets = () => (
    <>
      <Space style={{ marginBottom: 24 }} wrap>
        <Input
          prefix={<SearchOutlined />}
          placeholder={t('common.templates_search_presets')}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ width: 240 }}
          allowClear
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreatePreset}>
          {t('common.templates_create_preset')}
        </Button>
      </Space>

      {filteredPresets.length === 0 ? (
        <Empty description={t('common.templates_no_presets')} />
      ) : (
        <Row gutter={[16, 16]}>
          {filteredPresets.map((p) => (
            <Col xs={24} sm={12} lg={8} xl={6} key={p.id}>
              <Card
                hoverable
                onClick={() => { setSelectedPreset(p); setPresetOpen(true) }}
                cover={
                  p.previewImage ? (
                    <div style={{ height: 140, overflow: 'hidden', background: '#1a1a2e', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
                      <Image
                        src={p.previewImage} alt={p.name}
                        style={{ objectFit: 'cover', width: '100%', height: '100%', opacity: 0.7 }}
                        fallback={SVG_PLACEHOLDER_WORLDGEN}
                      />
                      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '16px', background: 'linear-gradient(transparent, rgba(0,0,0,0.8))' }}>
                        <span style={{ color: '#fff', fontWeight: 600, fontSize: 16 }}>{p.name}</span>
                      </div>
                    </div>
                  ) : (
                    <div style={{ height: 140, background: presetGradient(p.sortOrder ?? 0), display: 'flex', alignItems: 'flex-end', padding: 16 }}>
                      <span style={{ color: '#fff', fontWeight: 600, fontSize: 16, textShadow: '0 1px 3px rgba(0,0,0,0.3)' }}>{p.name}</span>
                    </div>
                  )
                }
              >
                <p style={{ color: '#888', height: 36, overflow: 'hidden', fontSize: 13, marginBottom: 8 }}>
                  {p.description || t('common.templates_presets_default_desc')}
                </p>
                <Space wrap size={[4, 4]}>
                  <SettingBadge setting="worldSize" value={p.worldSize || 'default'} />
                  <SettingBadge setting="dayMode" value={p.dayMode || 'default'} />
                  <SettingBadge setting="seasonStart" value={p.seasonStart || 'default'} />
                  <SettingBadge setting="branching" value={p.branching || 'default'} />
                </Space>
                <div style={{ marginTop: 8 }} onClick={(e) => e.stopPropagation()}>
                  <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => handleEditPreset(p)}>
                      {t('common.edit')}
                    </Button>
                    <Popconfirm title={t('common.templates_delete_confirm')} onConfirm={() => handleDeletePreset(p.id)}>
                      <Button size="small" danger icon={<DeleteOutlined />}>
                        {t('common.delete')}
                      </Button>
                    </Popconfirm>
                  </Space>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Modal
        title={null}
        open={presetOpen}
        onCancel={() => setPresetOpen(false)}
        footer={null}
        width={680}
        centered
        styles={{ body: { padding: 0 } }}
      >
        {selectedPreset && (
          <div>
            <div style={{
              background: presetGradient(selectedPreset.sortOrder ?? 0),
              padding: '32px 24px 24px',
              borderRadius: '8px 8px 0 0',
            }}>
              <h2 style={{ color: '#fff', margin: 0, fontWeight: 700, fontSize: 22, textShadow: '0 1px 3px rgba(0,0,0,0.3)' }}>
                {selectedPreset.name}
              </h2>
              {selectedPreset.description && (
                <p style={{ color: 'rgba(255,255,255,0.85)', margin: '8px 0 0', fontSize: 14 }}>
                  {selectedPreset.description}
                </p>
              )}
            </div>

            <div style={{ padding: 24 }}>
              <div style={{ marginBottom: 20 }}>
                <div style={{ fontSize: 13, color: '#888', marginBottom: 8, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 1 }}>
                  <GlobalOutlined style={{ marginRight: 6 }} />World Layout
                </div>
                <Row gutter={[12, 12]}>
                  {[
                    { icon: <GlobalOutlined />, label: t('common.templates_world_size'), value: tVal(selectedPreset.worldSize) },
                    { icon: <BranchesOutlined />, label: t('common.templates_branching'), value: tVal(selectedPreset.branching) },
                    { icon: <PartitionOutlined />, label: t('common.templates_loop'), value: tVal(selectedPreset.loopMode) },
                  ].map((s) => (
                    <Col span={8} key={s.label}>
                      <div style={{
                        background: '#fafafa', borderRadius: 8, padding: '12px 14px',
                        textAlign: 'center', border: '1px solid #f0f0f0',
                      }}>
                        <div style={{ color: '#1677ff', fontSize: 18, marginBottom: 4 }}>{s.icon}</div>
                        <div style={{ fontSize: 11, color: '#888', marginBottom: 2 }}>{s.label}</div>
                        <div style={{ fontSize: 14, fontWeight: 600 }}>{s.value}</div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </div>

              <div style={{ marginBottom: 20 }}>
                <div style={{ fontSize: 13, color: '#888', marginBottom: 8, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 1 }}>
                  <FieldTimeOutlined style={{ marginRight: 6 }} />Seasons & Day Cycle
                </div>
                <Row gutter={[12, 12]}>
                  {[
                    { icon: <SunOutlined />, label: t('common.templates_day_mode'), value: tVal(selectedPreset.dayMode) },
                    { icon: <FieldTimeOutlined />, label: t('common.templates_start_season'), value: tVal(selectedPreset.seasonStart) },
                    { icon: <FieldTimeOutlined />, label: t('common.templates_autumn'), value: tVal(selectedPreset.autumnLength) },
                    { icon: <FieldTimeOutlined />, label: t('common.templates_winter'), value: tVal(selectedPreset.winterLength) },
                    { icon: <FieldTimeOutlined />, label: t('common.templates_spring'), value: tVal(selectedPreset.springLength) },
                    { icon: <FieldTimeOutlined />, label: t('common.templates_summer'), value: tVal(selectedPreset.summerLength) },
                  ].map((s) => (
                    <Col span={8} key={s.label}>
                      <div style={{
                        background: '#fafafa', borderRadius: 8, padding: '12px 14px',
                        textAlign: 'center', border: '1px solid #f0f0f0',
                      }}>
                        <div style={{ color: '#fa8c16', fontSize: 18, marginBottom: 4 }}>{s.icon}</div>
                        <div style={{ fontSize: 11, color: '#888', marginBottom: 2 }}>{s.label}</div>
                        <div style={{ fontSize: 14, fontWeight: 600 }}>{s.value}</div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </div>

              <div>
                <div style={{ fontSize: 13, color: '#888', marginBottom: 8, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 1 }}>
                  <ExperimentOutlined style={{ marginRight: 6 }} />{t('common.templates_resources')}
                </div>
                <div style={{
                  background: '#fafafa', borderRadius: 8, padding: '16px',
                  border: '1px solid #f0f0f0', textAlign: 'center',
                }}>
                  <div style={{ fontSize: 18, color: '#722ed1', marginBottom: 4 }}><ExperimentOutlined /></div>
                  <div style={{ fontSize: 11, color: '#888', marginBottom: 4 }}>{t('common.templates_resources')}</div>
                  <div style={{ fontSize: 16, fontWeight: 600, color: '#722ed1' }}>
                    {tVal(selectedPreset.resourceVariety)}
                  </div>
                </div>
              </div>

              <div style={{ marginTop: 20 }}>
                <div style={{ fontSize: 13, color: '#888', marginBottom: 8, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 1 }}>
                  <SyncOutlined style={{ marginRight: 6 }} />Regrowth & Starting Gear
                </div>
                <Row gutter={[12, 12]}>
                  {[
                    { icon: <SyncOutlined />, label: t('common.templates_regrowth'), value: tVal('regrowth' in selectedPreset ? (selectedPreset as any).regrowth : undefined) },
                    { icon: <ToolOutlined />, label: t('common.templates_starting_gear'), value: tVal('startingGear' in selectedPreset ? (selectedPreset as any).startingGear : undefined) },
                  ].map((s) => (
                    <Col span={12} key={s.label}>
                      <div style={{ background: '#fafafa', borderRadius: 8, padding: '12px 14px', textAlign: 'center', border: '1px solid #f0f0f0' }}>
                        <div style={{ color: '#52c41a', fontSize: 18, marginBottom: 4 }}>{s.icon}</div>
                        <div style={{ fontSize: 11, color: '#888', marginBottom: 2 }}>{s.label}</div>
                        <div style={{ fontSize: 14, fontWeight: 600 }}>{s.value}</div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </div>

              <div style={{ marginTop: 20 }}>
                <div style={{ fontSize: 13, color: '#888', marginBottom: 8, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 1 }}>
                  <BugOutlined style={{ marginRight: 6 }} />Creatures & World Features
                </div>
                <Row gutter={[12, 12]}>
                  {[
                    { icon: <BugOutlined />, label: t('common.templates_creatures'), value: tVal('creatures' in selectedPreset ? (selectedPreset as any).creatures : undefined) },
                    { icon: <GiftOutlined />, label: t('common.templates_boons'), value: tVal('boons' in selectedPreset ? (selectedPreset as any).boons : undefined) },
                    { icon: <CloudOutlined />, label: t('common.templates_weather'), value: tVal('weather' in selectedPreset ? (selectedPreset as any).weather : undefined) },
                  ].map((s) => (
                    <Col span={8} key={s.label}>
                      <div style={{ background: '#fafafa', borderRadius: 8, padding: '12px 14px', textAlign: 'center', border: '1px solid #f0f0f0' }}>
                        <div style={{ color: '#eb2f96', fontSize: 18, marginBottom: 4 }}>{s.icon}</div>
                        <div style={{ fontSize: 11, color: '#888', marginBottom: 2 }}>{s.label}</div>
                        <div style={{ fontSize: 14, fontWeight: 600 }}>{s.value}</div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </>
  )

  const renderWorkshop = () => {
    const hintType = keyword
      ? t('common.templates_search_results')
      : t('common.templates_popular_mods')

    return (
      <>
        <Space style={{ marginBottom: 24 }} wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder={t('common.templates_search_mods')}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ width: 280 }}
            allowClear
            onPressEnter={fetchWorkshop}
          />
          <Button type="primary" onClick={fetchWorkshop} icon={<SearchOutlined />}>{t('common.templates_search_button')}</Button>
        </Space>

        <div style={{ marginBottom: 16, color: '#888', fontSize: 13 }}>
          <FireOutlined style={{ color: '#ff4d4f', marginRight: 4 }} />
          {t('common.templates_workshop_search_hint', { type: hintType })}
        </div>

        {workshopMods.length === 0 ? (
          <Empty description={t('common.templates_no_mods')} />
        ) : (
          <Row gutter={[16, 16]}>
            {workshopMods.map((mod) => (
              <Col xs={24} sm={12} lg={8} xl={6} key={mod.workshopId}>
                <Card
                  hoverable
                  cover={
                    mod.previewUrl ? (
                      <div style={{ height: 140, overflow: 'hidden', background: '#f5f5f5' }}>
                        <Image
                          src={mod.previewUrl} alt={mod.title}
                          style={{ objectFit: 'cover', width: '100%', height: '100%' }}
                          fallback={SVG_PLACEHOLDER_MOD}
                        />
                      </div>
                    ) : (
                      <div style={{ height: 140, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <ExperimentOutlined style={{ fontSize: 40, color: '#d9d9d9' }} />
                      </div>
                    )
                  }
                >
                  <Card.Meta
                    title={<span style={{ fontSize: 14 }}>{mod.title}</span>}
                    description={
                      <>
                        <p style={{ color: '#888', height: 32, overflow: 'hidden', fontSize: 12, marginBottom: 8 }}>
                          {mod.description || t('common.templates_desc_default')}
                        </p>
                        <Space>
                          <Badge count={mod.subscriptions} overflowCount={999999} style={{ backgroundColor: '#52c41a' }} title={t('common.templates_subscriptions')} />
                          <Tooltip title={t('common.templates_subscriptions')}><DownloadOutlined /></Tooltip>
                          <Divider type="vertical" />
                          <Badge count={mod.favorited} overflowCount={99999} style={{ backgroundColor: '#faad14' }} title={t('common.templates_favorited')} />
                          <Tooltip title={t('common.templates_favorited')}><StarOutlined /></Tooltip>
                        </Space>
                      </>
                    }
                  />
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </>
    )
  }

  const renderDetailModal = () => (
    <Modal
      title={detailData?.template.name || t('common.templates_detail_title')}
      open={detailOpen}
      onCancel={() => { setDetailOpen(false); setDetailData(null) }}
      footer={null}
      width={720}
    >
      {detailLoading ? (
        <Skeleton active />
      ) : detailData ? (
        <>
          <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('common.templates_category')}>{detailData.template.category || '-'}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_game_mode')}>{detailData.template.gameMode || '-'}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_max_players')}>{detailData.template.maxPlayers || 6}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_version')}>v{detailData.template.version || 1}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_downloads')}>{detailData.template.downloadCount || 0}</Descriptions.Item>
            <Descriptions.Item label="Rating">
              <Rate disabled value={Math.round(detailData.template.ratingAvg || 0)} style={{ fontSize: 12 }} />
              <span style={{ marginLeft: 4 }}>({detailData.template.ratingCount || 0})</span>
            </Descriptions.Item>
          </Descriptions>

          {detailData.template.description && (
            <div style={{ marginBottom: 16 }}>
              <strong>{t('common.templates_desc_default')}:</strong>
              <p style={{ color: '#888' }}>{detailData.template.description}</p>
            </div>
          )}

          {detailData.worldGenPresets && detailData.worldGenPresets.length > 0 && (
            <>
              <Divider orientation="left">{t('common.templates_bound_presets')}</Divider>
              <Row gutter={[12, 12]}>
                {detailData.worldGenPresets.map((wp) => (
                  <Col span={12} key={wp.id}>
                    <Card size="small" title={wp.name}>
                      <Space wrap size={[4, 4]}>
                        <Tag color="blue">{wp.worldSize || 'default'}</Tag>
                        <Tag>{wp.dayMode || 'default'}</Tag>
                        <Tag>{wp.seasonStart || 'default'}</Tag>
                      </Space>
                    </Card>
                  </Col>
                ))}
              </Row>
            </>
          )}
        </>
      ) : (
        <Empty description={t('common.templates_detail_not_found')} />
      )}
    </Modal>
  )

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>{t('common.templates_title')}</h2>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => { setActiveTab(key); setKeyword(''); setCategory(undefined) }}
        items={[
          {
            key: 'server',
            label: <span><RocketOutlined /> {t('common.templates_server')}</span>,
            children: renderServerTemplates(),
          },
          {
            key: 'presets',
            label: <span><GlobalOutlined /> {t('common.templates_world_gen')}</span>,
            children: renderWorldGenPresets(),
          },
          {
            key: 'workshop',
            label: <span><ExperimentOutlined /> {t('common.templates_workshop')}</span>,
            children: renderWorkshop(),
          },
        ]}
        style={{ minHeight: 400 }}
      />

      {renderDetailModal()}
      <TemplateFormModal
        open={templateFormOpen}
        initialValues={editingTemplate}
        onClose={() => setTemplateFormOpen(false)}
        onSaved={() => { fetchTemplates(); fetchPresets() }}
      />
      <WorldGenPresetFormModal
        open={presetFormOpen}
        initialValues={editingPreset}
        onClose={() => setPresetFormOpen(false)}
        onSaved={() => fetchPresets()}
      />
    </div>
  )
}
