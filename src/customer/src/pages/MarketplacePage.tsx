import { useEffect, useState, useCallback } from 'react'
import { Space, message, Modal, Descriptions, Rate, Divider, Row, Col, Card, Tag } from 'antd'
import { CloudServerOutlined } from '@ant-design/icons'
import { browseTemplates, forkTemplate, getTemplateDetail, type TemplateInfo, type TemplateFullDetail } from '../api/templates'
import { useTranslation } from '../i18n'
import SearchFilterBar, { CATEGORY_OPTIONS } from '../components/SearchFilterBar'
import AsyncContentView, { CardGrid } from '../components/AsyncContentView'
import ResourceCard from '../components/ResourceCard'
import { CARD_GRADIENT } from '../constants/dst'

export default function MarketplacePage() {
  const [templates, setTemplates] = useState<TemplateInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<string | undefined>()
  const [sort, setSort] = useState('downloads')
  const [detailOpen, setDetailOpen] = useState(false)
  const [detail, setDetail] = useState<TemplateFullDetail | null>(null)
  const { t } = useTranslation()

  const fetch = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { sort }
      if (category) params.category = category
      const res = await browseTemplates(params)
      if (res.code === 0) setTemplates(res.data?.records ?? [])
    } catch { message.error('Failed to load marketplace') }
    finally { setLoading(false) }
  }, [category, sort])

  useEffect(() => { fetch() }, [fetch])

  const handleFork = async (id: number) => {
    try {
      const res = await forkTemplate(id)
      if (res.code === 0) { message.success(t('common.templates_fork_success')); fetch() }
    } catch { message.error(t('common.templates_fork_failed')) }
  }

  const handleViewDetail = async (id: number) => {
    try {
      const res = await getTemplateDetail(id)
      if (res.code === 0 && res.data) { setDetail(res.data); setDetailOpen(true) }
    } catch { message.error('Failed to load detail') }
  }

  const filtered = templates.filter((c) =>
    !keyword || c.name.toLowerCase().includes(keyword.toLowerCase()) ||
    (c.tags && c.tags.toLowerCase().includes(keyword.toLowerCase())) ||
    (c.description && c.description.toLowerCase().includes(keyword.toLowerCase()))
  )

  return (
    <div>
      <h2 style={{ marginBottom: 24, fontSize: 24, fontWeight: 700, letterSpacing: '-.5px' }}>Config Marketplace</h2>
      <SearchFilterBar keyword={keyword} onKeywordChange={setKeyword} category={category} onCategoryChange={setCategory} sort={sort} onSortChange={setSort}
        categories={CATEGORY_OPTIONS.map((o) => ({ value: o.value, label: t(`common.templates_catval_${o.value}`) || o.label }))} />
      <AsyncContentView loading={loading} isEmpty={filtered.length === 0} emptyDescription="No configs found">
        <CardGrid>
          {filtered.map((c) => (
            <ResourceCard key={c.id}
              title={c.name}
              actions={[]}
              description={c.description || t('common.templates_desc_default')}
              coverImage={c.coverImage}
              coverGradient={CARD_GRADIENT.server}
              coverIcon={<CloudServerOutlined style={{ fontSize: 40, color: 'rgba(255,255,255,0.6)' }} />}
              tags={[
                { label: c.templateType === 'server_template' ? t('common.templates_server') : t('common.templates_world_gen'), color: 'blue' },
                { label: c.category || t('common.templates_category_general') },
                { label: c.gameMode || 'survival' },
              ]}
              rating={c.ratingAvg ? { avg: c.ratingAvg, count: c.ratingCount || 0 } : undefined}
              verified={!!c.verified}
              verifiedLabel={t('common.templates_verified')}
              onViewDetail={() => handleViewDetail(c.id)}
              onFork={() => handleFork(c.id)}
              forkCount={c.downloadCount || 0}
              onDeploy={() => window.location.href = `/servers/deploy?templateId=${c.id}`}
            />
          ))}
        </CardGrid>
      </AsyncContentView>
      <Modal title={detail?.template.name} open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={640}>
        {detail && <>
          <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
            <Descriptions.Item label={t('common.templates_category')}>{detail.template.category||'-'}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_game_mode')}>{detail.template.gameMode||'-'}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_max_players')}>{detail.template.maxPlayers||6}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_version')}>v{detail.template.version||1}</Descriptions.Item>
            <Descriptions.Item label={t('common.templates_downloads')}>{detail.template.downloadCount||0}</Descriptions.Item>
            <Descriptions.Item label="Rating"><Rate disabled value={Math.round(detail.template.ratingAvg||0)} style={{ fontSize: 12 }}/><span style={{ marginLeft: 4 }}>({detail.template.ratingCount||0})</span></Descriptions.Item>
          </Descriptions>
          {detail.template.description && <p style={{ color: '#8c8c8c', marginBottom: 16 }}>{detail.template.description}</p>}
          {detail.worldGenPresets && detail.worldGenPresets.length > 0 && <><Divider>{t('common.templates_bound_presets')}</Divider><Row gutter={[8,8]}>{detail.worldGenPresets.map((wp) => <Col span={12} key={wp.id}><Card size="small" title={wp.name}><Space wrap size={[4,4]}><Tag>{wp.worldSize}</Tag><Tag>{wp.dayMode}</Tag></Space></Card></Col>)}</Row></>}
        </>}
      </Modal>
    </div>
  )
}
