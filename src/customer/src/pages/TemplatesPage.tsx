import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Tag, Space, Tabs, message, Image, Button, Modal, Descriptions, Rate, Tooltip, Divider, Popconfirm, Collapse, Row, Col, Empty, Skeleton } from 'antd'
import {
  ForkOutlined, RocketOutlined,
  StarOutlined, EyeOutlined, ExperimentOutlined,
  GlobalOutlined, BranchesOutlined,
  FieldTimeOutlined, SunOutlined, PartitionOutlined, SettingOutlined,
  EditOutlined, DeleteOutlined, PlusOutlined,
  AppstoreOutlined
} from '@ant-design/icons'
import {
  browseTemplates, forkTemplate, getTemplateDetail,
  browseWorldGenPresets,
  deleteTemplate, deleteWorldGenPreset,
  publishTemplate, unpublishTemplate,
  type TemplateInfo, type WorldGenPresetInfo,
  type TemplateFullDetail
} from '../api/templates'
import { useTranslation } from '../i18n'
import { useAuth } from '../context/AuthContext'
import ServerTemplateFormModal from '../components/ServerTemplateFormModal'
import WorldGenPresetFormModal from '../components/WorldGenPresetFormModal'
import ModpackTemplateFormModal from '../components/ModpackTemplateFormModal'
import SearchFilterBar from '../components/SearchFilterBar'
import AsyncContentView, { CardGrid } from '../components/AsyncContentView'

const SVG_PLACEHOLDER_TEMPLATE = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjYmZiZmJmIiBmb250LXNpemU9IjI0Ij5UZW1wbGF0ZTwvdGV4dD48L3N2Zz4='
const SVG_PLACEHOLDER_WORLDGEN = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjMWExYTJlIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjZmZmIiBmb250LXNpemU9IjI4Ij7wn4yN88K/PC90ZXh0Pjwvc3ZnPg=='

const PRESET_COLORS = ['#1677ff','#52c41a','#fa8c16','#13c2c2','#722ed1','#2f54eb','#f5222d','#faad14','#434343','#eb2f96','#a0d911','#fa541c']
function presetGradient(index: number) { const c = PRESET_COLORS[index % PRESET_COLORS.length]; return `linear-gradient(135deg, ${c} 0%, ${c}88 100%)` }

// ============ Shared card style constants ============
const CARD_STYLE: React.CSSProperties = { borderRadius: 12, overflow: 'hidden', border: '1px solid #f0f0f0', transition: 'all .25s' }
const COVER_STYLE: React.CSSProperties = { height: 150, display: 'flex', alignItems: 'center', justifyContent: 'center' }
const GRADIENT_SERVER = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
const GRADIENT_MODPACK = 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'

// ============ Setting badge for world gen ============
const settingIcons: Record<string, React.ReactNode> = { worldSize: <GlobalOutlined />, branching: <BranchesOutlined />, loopMode: <PartitionOutlined />, seasonStart: <FieldTimeOutlined />, dayMode: <SunOutlined />, autumnLength: <FieldTimeOutlined />, winterLength: <FieldTimeOutlined />, springLength: <FieldTimeOutlined />, summerLength: <FieldTimeOutlined />, resourceVariety: <ExperimentOutlined /> }
const settingLabels: Record<string, string> = { worldSize: 'World Size', branching: 'Branching', loopMode: 'Loop', seasonStart: 'Starting Season', dayMode: 'Day/Night', autumnLength: 'Autumn', winterLength: 'Winter', springLength: 'Spring', summerLength: 'Summer', resourceVariety: 'Resources' }
function SettingBadge({ setting, value }: { setting: string; value: string }) {
  const { t } = useTranslation()
  const label = t(`common.${setting.replace(/([A-Z])/g, '_$1').toLowerCase()}`) || settingLabels[setting] || setting
  return <Tooltip title={`${label}: ${value}`}><Tag icon={settingIcons[setting]} color="blue">{value}</Tag></Tooltip>
}

export default function TemplatesPage() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const { state } = useAuth()
  const currentUserId = state.userInfo?.userId
  const tVal = (v: string | undefined) => { if (!v || v === 'default') return t('common.templates_val_default'); const key = `templates_val_${v.replace(/-/g, '').replace(/_/g, '')}`; const tr = t(`common.${key}`); return tr !== `common.${key}` ? tr : v }
  const [activeTab, setActiveTab] = useState('server')
  const [loading, setLoading] = useState(true)
  const [templates, setTemplates] = useState<TemplateInfo[]>([])
  const [presets, setPresets] = useState<WorldGenPresetInfo[]>([])
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<string | undefined>()
  const [sort, setSort] = useState('downloads')
  const [detailOpen, setDetailOpen] = useState(false); const [detailData, setDetailData] = useState<TemplateFullDetail | null>(null); const [detailLoading, setDetailLoading] = useState(false)
  const [presetOpen, setPresetOpen] = useState(false); const [selectedPreset, setSelectedPreset] = useState<WorldGenPresetInfo | null>(null)
  const [templateFormOpen, setTemplateFormOpen] = useState(false); const [modTemplateFormOpen, setModTemplateFormOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<TemplateInfo | null>(null)
  const [presetFormOpen, setPresetFormOpen] = useState(false); const [editingPreset, setEditingPreset] = useState<WorldGenPresetInfo | null>(null)

  const fetchTemplates = useCallback(async () => { setLoading(true); try { const params: Record<string, unknown> = { sort, type: activeTab === 'server' ? 'server_template' : 'modpack' }; if (category) params.category = category; const res = await browseTemplates(params); if (res.code === 0) setTemplates(res.data?.records ?? []) } catch { message.error(t('common.templates_load_failed')) } finally { setLoading(false) } }, [category, sort, activeTab, t])
  const fetchPresets = useCallback(async () => { try { const res = await browseWorldGenPresets(); if (res.code === 0) setPresets(res.data?.records ?? []) } catch { message.error(t('common.templates_presets_load_failed')) } }, [t])
  useEffect(() => { if (activeTab === 'server' || activeTab === 'workshop') fetchTemplates(); else if (activeTab === 'presets') fetchPresets() }, [activeTab, fetchTemplates, fetchPresets])

  const handleViewDetail = async (id: number) => { setDetailLoading(true); try { const res = await getTemplateDetail(id); if (res.code === 0) { setDetailData(res.data ?? null); setDetailOpen(true) } } catch { message.error(t('common.templates_detail_failed')) } finally { setDetailLoading(false) } }
  const handleFork = async (id: number) => { try { const res = await forkTemplate(id); if (res.code === 0) { message.success(t('common.templates_fork_success')); fetchTemplates() } else message.error(t('common.templates_fork_failed')) } catch { message.error(t('common.templates_fork_failed')) } }
  const handleDeploy = (id: number) => navigate('/servers/deploy', { state: { templateId: id } })
  const handleDelete = async (id: number) => { try { const res = await deleteTemplate(id); if (res.code === 0) { message.success(t('common.templates_deleted')); fetchTemplates() } else message.error(res.message) } catch { message.error('Delete failed') } }
  const handleDeletePreset = async (id: number) => { try { const res = await deleteWorldGenPreset(id); if (res.code === 0) { message.success(t('common.templates_deleted')); fetchPresets() } else message.error(res.message) } catch { message.error('Delete failed') } }
  const handlePublish = async (id: number) => { try { const res = await publishTemplate(id); if (res.code === 0) { message.success(t('common.templates_published')); fetchTemplates() } else message.error(res.message) } catch { message.error('Publish failed') } }
  const handleUnpublish = async (id: number) => { try { const res = await unpublishTemplate(id); if (res.code === 0) { message.success(t('common.templates_unpublished')); fetchTemplates() } else message.error(res.message) } catch { message.error('Unpublish failed') } }
  const handleCreateTemplate = () => { setEditingTemplate(null); if (activeTab === 'workshop') setModTemplateFormOpen(true); else setTemplateFormOpen(true) }
  const handleEditTemplate = (t2: TemplateInfo) => { setEditingTemplate(t2); if (t2.templateType === 'modpack') setModTemplateFormOpen(true); else setTemplateFormOpen(true) }
  const handleCreatePreset = () => { setEditingPreset(null); setPresetFormOpen(true) }

  const filtered = (list: any[], fields: string[]) => list.filter((item) => !keyword || fields.some((f) => (item[f] || '').toLowerCase().includes(keyword.toLowerCase())))

  // ============ Card component ============
  const TemplateCard = ({ t2 }: { t2: TemplateInfo }) => {
    const isOwn = currentUserId && currentUserId === t2.authorId
    const gradient = t2.templateType === 'modpack' ? GRADIENT_MODPACK : GRADIENT_SERVER
    return (
      <Col xs={24} sm={12} lg={8} xl={6} key={t2.id}>
        <Card hoverable style={CARD_STYLE}
          cover={t2.coverImage ? <div style={{ ...COVER_STYLE, background: '#f5f5f5', overflow: 'hidden' }}><Image src={t2.coverImage} alt={t2.name} style={{ objectFit: 'cover', width: '100%', height: '100%' }} fallback={SVG_PLACEHOLDER_TEMPLATE}/></div>
            : <div style={{ ...COVER_STYLE, background: gradient }}><SettingOutlined style={{ fontSize: 42, color: 'rgba(255,255,255,.5)' }}/></div>}
          actions={[
            <Tooltip title={t('common.templates_view_detail')} key="v"><EyeOutlined onClick={() => handleViewDetail(t2.id)}/></Tooltip>,
            <Tooltip title={`${t('common.templates_fork')} (${t2.downloadCount||0})`} key="fk"><Space><ForkOutlined onClick={() => handleFork(t2.id)}/><span>{t2.downloadCount||0}</span></Space></Tooltip>,
            <Tooltip title={t('common.templates_deploy')} key="dp"><RocketOutlined onClick={() => handleDeploy(t2.id)}/></Tooltip>,
            ...(isOwn ? [
              <Tooltip title={t('common.edit')} key="ed"><EditOutlined onClick={() => handleEditTemplate(t2)}/></Tooltip>,
              t2.status === 'published' ? <Tooltip title={t('common.templates_unpublish')} key="up"><StarOutlined onClick={() => handleUnpublish(t2.id)} style={{ color: '#faad14' }}/></Tooltip>
                : <Tooltip title={t('common.templates_publish')} key="pb"><RocketOutlined onClick={() => handlePublish(t2.id)}/></Tooltip>,
              <Popconfirm key="dl" title={t('common.templates_delete_confirm')} onConfirm={() => handleDelete(t2.id)}><DeleteOutlined/></Popconfirm>,
            ] : []),
          ]}
        >
          <Card.Meta title={<Space size={4}>{t2.name}{t2.verified ? <Tag color="gold" style={{ fontSize: 10, lineHeight:'16px' }}>{t('common.templates_verified')}</Tag>:null}</Space>}
            description={<>
              <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 8, lineHeight: '18px', height: 36, overflow: 'hidden' }}>{t2.description || t('common.templates_desc_default')}</div>
              <Space wrap size={[0,4]}><Tag color="blue">{t2.category || t('common.templates_category_general')}</Tag><Tag>{t2.gameMode||'survival'}</Tag><Tag>{t2.maxPlayers||6}p</Tag></Space>
              <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 4 }}><Rate disabled value={Math.round(t2.ratingAvg||0)} style={{ fontSize: 12 }}/><span style={{ color: '#8c8c8c', fontSize: 12 }}>({t2.ratingCount||0})</span></div>
            </>}/>
        </Card>
      </Col>
    )
  }

  const PresetCard = ({ p }: { p: WorldGenPresetInfo }) => (
    <Col xs={24} sm={12} lg={8} xl={6} key={p.id}>
      <Card hoverable style={CARD_STYLE} onClick={() => { setSelectedPreset(p); setPresetOpen(true) }}
        cover={p.previewImage ? <div style={{ ...COVER_STYLE, background: '#1a1a2e', position: 'relative', overflow: 'hidden' }}><Image src={p.previewImage} alt={p.name} style={{ objectFit: 'cover', width: '100%', height: '100%', opacity: .7 }} fallback={SVG_PLACEHOLDER_WORLDGEN}/><div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: 20, background: 'linear-gradient(transparent, rgba(0,0,0,.85))' }}><span style={{ color: '#fff', fontWeight: 600, fontSize: 15 }}>{p.name}</span></div></div>
          : <div style={{ ...COVER_STYLE, background: presetGradient(p.sortOrder??0), alignItems: 'flex-end', padding: 20 }}><span style={{ color: '#fff', fontWeight: 600, fontSize: 15, textShadow: '0 1px 3px rgba(0,0,0,.3)' }}>{p.name}</span></div>}
      >
        <div style={{ color: '#8c8c8c', fontSize: 12, lineHeight: '18px', height: 36, overflow: 'hidden', marginBottom: 8 }}>{p.description || t('common.templates_presets_default_desc')}</div>
        <Space wrap size={[4,4]}><SettingBadge setting="worldSize" value={p.worldSize||'default'}/><SettingBadge setting="dayMode" value={p.dayMode||'default'}/><SettingBadge setting="seasonStart" value={p.seasonStart||'default'}/><SettingBadge setting="branching" value={p.branching||'default'}/></Space>
        <div style={{ marginTop: 10 }} onClick={(e) => e.stopPropagation()}>
          <Space>
            <Button size="small" icon={<EditOutlined/>} onClick={() => { setEditingPreset(p); setPresetFormOpen(true) }}>{t('common.edit')}</Button>
            <Button size="small" danger icon={<DeleteOutlined/>} onClick={() => handleDeletePreset(p.id)}>{t('common.delete')}</Button>
          </Space>
        </div>
      </Card>
    </Col>
  )

  // ============ Tab content ============
  const renderServerTab = () => {
    const list = activeTab === 'server' ? templates.filter((t2) => t2.templateType !== 'modpack') : templates.filter((t2) => t2.templateType === 'modpack')
    const filteredList = filtered(list, ['name', 'description'])
    return <>
      <SearchFilterBar keyword={keyword} onKeywordChange={setKeyword} category={category} onCategoryChange={setCategory} sort={sort} onSortChange={setSort}
        extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreateTemplate}>{t('common.templates_create_template')}</Button>} />
      <AsyncContentView loading={loading} isEmpty={filteredList.length === 0} emptyDescription={t('common.templates_no_results')}>
        <CardGrid>{filteredList.map((t2) => <TemplateCard key={t2.id} t2={t2}/>)}</CardGrid>
      </AsyncContentView>
    </>
  }

  const renderPresetsTab = () => {
    const filteredPresets = filtered(presets, ['name', 'description'])
    return <>
      <SearchFilterBar keyword={keyword} onKeywordChange={setKeyword} searchPlaceholder={t('common.templates_search_presets')}
        extra={<Button type="primary" icon={<PlusOutlined/>} onClick={handleCreatePreset}>{t('common.templates_create_preset')}</Button>} />
      <AsyncContentView loading={false} isEmpty={filteredPresets.length === 0} emptyDescription={t('common.templates_no_presets')}>
        <CardGrid>{filteredPresets.map((p) => <PresetCard key={p.id} p={p}/>)}</CardGrid>
      </AsyncContentView>
    </>
  }

  // ============ Modals ============
  const renderDetailModal = () => (
    <Modal title={detailData?.template.name} open={detailOpen} onCancel={() => { setDetailOpen(false); setDetailData(null) }} footer={null} width={680} styles={{ body: { padding: '16px 24px' } }}>
      {detailLoading ? <Skeleton active/> : detailData ? <>
        <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
          <Descriptions.Item label={t('common.templates_category')}>{detailData.template.category||'-'}</Descriptions.Item>
          <Descriptions.Item label={t('common.templates_game_mode')}>{detailData.template.gameMode||'-'}</Descriptions.Item>
          <Descriptions.Item label={t('common.templates_max_players')}>{detailData.template.maxPlayers||6}</Descriptions.Item>
          <Descriptions.Item label={t('common.templates_version')}>v{detailData.template.version||1}</Descriptions.Item>
          <Descriptions.Item label={t('common.templates_downloads')}>{detailData.template.downloadCount||0}</Descriptions.Item>
          <Descriptions.Item label="Rating"><Rate disabled value={Math.round(detailData.template.ratingAvg||0)} style={{ fontSize: 12 }}/><span style={{ marginLeft: 4 }}>({detailData.template.ratingCount||0})</span></Descriptions.Item>
        </Descriptions>
        {detailData.template.description && <div style={{ marginBottom: 16 }}><strong>{t('common.templates_description')}:</strong><p style={{ color: '#8c8c8c', marginTop: 4 }}>{detailData.template.description}</p></div>}
        {detailData.worldGenPresets && detailData.worldGenPresets.length > 0 && <><Divider>{t('common.templates_bound_presets')}</Divider><Row gutter={[8,8]}>{detailData.worldGenPresets.map((wp) => <Col span={12} key={wp.id}><Card size="small" title={wp.name}><Space wrap size={[4,4]}><Tag color="blue">{wp.worldSize}</Tag><Tag>{wp.dayMode}</Tag><Tag>{wp.seasonStart}</Tag></Space></Card></Col>)}</Row></>}
      </> : <Empty description={t('common.templates_detail_not_found')}/>}
    </Modal>
  )

  const renderPresetDetailModal = () => (
    <Modal title={null} open={presetOpen} onCancel={() => setPresetOpen(false)} footer={null} width={680} centered styles={{ body: { padding: 0 } }}>
      {selectedPreset && <div>
        <div style={{ background: presetGradient(selectedPreset.sortOrder??0), padding: '32px 24px 24px', borderRadius: '8px 8px 0 0' }}>
          <h2 style={{ color: '#fff', margin: 0, fontWeight: 700, fontSize: 22, textShadow: '0 1px 3px rgba(0,0,0,.3)' }}>{selectedPreset.name}</h2>
          {selectedPreset.description && <p style={{ color: 'rgba(255,255,255,.85)', margin: '8px 0 0', fontSize: 14 }}>{selectedPreset.description}</p>}
        </div>
        <div style={{ padding: 24 }}>
          {[{ title: 'World Layout', icon: <GlobalOutlined/>, color: '#1677ff', items: [{ icon: <GlobalOutlined/>, label: 'World Size', val: tVal(selectedPreset.worldSize) },{ icon: <BranchesOutlined/>, label: 'Branching', val: tVal(selectedPreset.branching) },{ icon: <PartitionOutlined/>, label: 'Loop', val: tVal(selectedPreset.loopMode) }]},
            { title: 'Seasons & Day', icon: <FieldTimeOutlined/>, color: '#fa8c16', items: [{ icon: <SunOutlined/>, label: 'Day Mode', val: tVal(selectedPreset.dayMode) },{ icon: <FieldTimeOutlined/>, label: 'Start Season', val: tVal(selectedPreset.seasonStart) },{ icon: <FieldTimeOutlined/>, label: 'Autumn', val: tVal(selectedPreset.autumnLength) },{ icon: <FieldTimeOutlined/>, label: 'Winter', val: tVal(selectedPreset.winterLength) },{ icon: <FieldTimeOutlined/>, label: 'Spring', val: tVal(selectedPreset.springLength) },{ icon: <FieldTimeOutlined/>, label: 'Summer', val: tVal(selectedPreset.summerLength) }]},
            { title: 'Resources', icon: <ExperimentOutlined/>, color: '#722ed1', items: [{ icon: <ExperimentOutlined/>, label: 'Variety', val: tVal(selectedPreset.resourceVariety) }]},
          ].map((section) => (
            <div key={section.title} style={{ marginBottom: 20 }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: '#8c8c8c', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 }}>{section.icon} {section.title}</div>
              <Row gutter={[12,12]}>{section.items.map((s) => <Col span={section.items.length <= 3 ? 8 : 8} key={s.label}><div style={{ background: '#fafafa', borderRadius: 10, padding: '14px 12px', textAlign: 'center', border: '1px solid #f0f0f0' }}><div style={{ color: section.color, fontSize: 18, marginBottom: 4 }}>{s.icon}</div><div style={{ fontSize: 11, color: '#8c8c8c', marginBottom: 2 }}>{s.label}</div><div style={{ fontSize: 13, fontWeight: 600 }}>{s.val}</div></div></Col>)}</Row>
            </div>
          ))}
          {(() => { let extra: Record<string,string> = {}; try { if ((selectedPreset as any).extraSettings) extra = typeof (selectedPreset as any).extraSettings === 'string' ? JSON.parse((selectedPreset as any).extraSettings) : (selectedPreset as any).extraSettings } catch {}; const keys = Object.keys(extra); if (keys.length === 0) return null; return <div style={{ marginTop: 20 }}><Collapse items={[{ key: 'extra', label: <span style={{ fontSize: 13, fontWeight: 500 }}>{t('common.templates_advanced_settings')} ({keys.length})</span>, children: <Row gutter={[8,8]}>{keys.map((k) => <Col span={8} key={k}><div style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#fafafa', borderRadius: 8, padding: '6px 10px' }}><Image src={`/images/worldgen/${k}.png`} fallback="/images/worldgen/default.png" preview={false} width={28} height={28} style={{ borderRadius: 4 }}/><div><div style={{ fontSize: 10, color: '#8c8c8c' }}>{k}</div><div style={{ fontSize: 12, fontWeight: 500 }}>{tVal(extra[k])}</div></div></div></Col>)}</Row>}]} style={{ background: '#fff' }}/></div> })()}
        </div>
      </div>}
    </Modal>
  )

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, letterSpacing: '-.5px' }}>{t('common.templates_title')}</h1>
        <p style={{ margin: '4px 0 0', color: '#8c8c8c', fontSize: 14 }}>Manage your server configurations, world generation presets, and mod collections.</p>
      </div>
      <Tabs activeKey={activeTab} onChange={(key) => { setActiveTab(key); setKeyword(''); setCategory(undefined) }}
        style={{ minHeight: 400 }}
        items={[
          { key: 'server', label: <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}><AppstoreOutlined style={{ fontSize: 16 }}/>{t('common.templates_server')}</span>, children: renderServerTab() },
          { key: 'presets', label: <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}><GlobalOutlined style={{ fontSize: 16 }}/>{t('common.templates_world_gen')}</span>, children: renderPresetsTab() },
          { key: 'workshop', label: <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}><ExperimentOutlined style={{ fontSize: 16 }}/>{t('common.templates_workshop')}</span>, children: renderServerTab() },
        ]}
      />
      {renderDetailModal()}{renderPresetDetailModal()}
      <ServerTemplateFormModal open={templateFormOpen} initialValues={editingTemplate} onClose={() => setTemplateFormOpen(false)} onSaved={fetchTemplates}/>
      <ModpackTemplateFormModal open={modTemplateFormOpen} initialValues={editingTemplate} onClose={() => setModTemplateFormOpen(false)} onSaved={fetchTemplates}/>
      <WorldGenPresetFormModal open={presetFormOpen} initialValues={editingPreset} onClose={() => setPresetFormOpen(false)} onSaved={fetchPresets}/>
    </div>
  )
}
