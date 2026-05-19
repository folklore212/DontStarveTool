import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, Row, Col, Collapse, Image } from 'antd'
import { PictureOutlined } from '@ant-design/icons'
import {
  createWorldGenPreset, updateWorldGenPreset,
  type WorldGenPresetInfo, type WorldGenMetadata,
} from '../api/templates'
import { useTranslation } from '../i18n'
import { WORLD_GEN_METADATA } from './worldGenMetadata'
import useResourceForm from '../hooks/useResourceForm'

const FIXED_COLUMNS = [
  'worldSize', 'branching', 'loop', 'season_start', 'day',
  'autumn', 'winter', 'spring', 'summer', 'resourceVariety',
]

// DST-correct per-setting defaults (different from generic "default")
const DST_DEFAULTS: Record<string, string> = {
  basicresource_regrowth: 'always',  // enabled
  extrastartingitems: '0',           // always
  ghostenabled: 'always',            // become ghost
  portalresurection: 'always',       // enabled
  spawnmode: 'fixed',                // portal
  has_ocean: 'true',
  keep_disconnected_tiles: 'true',
  no_joining_islands: 'true',
  no_wormholes_to_disconnected_tiles: 'true',
  layout_mode: 'LinkNodesByKeys',
  wormhole_prefab: 'wormhole',
  specialevent: 'default',           // auto
  healthpenalty: 'always',           // enabled
  ghostsanitydrain: 'none',          // disabled by default
  ocean_seastack: 'ocean_default',
  ocean_waterplant: 'ocean_default',
  prefabswaps: 'normal',
  task_set: 'cave_default',          // cave biome
  start_location: 'caves',           // cave spawn
  wildfires: 'never',                // no wildfires in caves
}

interface Props {
  open: boolean
  initialValues?: WorldGenPresetInfo | null
  onClose: () => void
  onSaved: () => void
}

export default function WorldGenPresetFormModal({ open, initialValues, onClose, onSaved }: Props) {
  const [form] = Form.useForm()
  const metadata = WORLD_GEN_METADATA as WorldGenMetadata
  const [previewUrl, setPreviewUrl] = useState('')
  const [presetType, setPresetType] = useState('surface')
  const { t } = useTranslation()

  const { loading, editing, handleSubmit } = useResourceForm({
    form, initialValues, onSaved, onClose,
    async onSubmit(values, _isEdit): Promise<{ code: number; message?: string }> {
      const allValues = { ...values, previewImage: previewUrl } as Record<string, unknown>
      const fixed: Record<string, unknown> = {}
      const extra: Record<string, unknown> = {}
      for (const [k, v] of Object.entries(allValues)) {
        if (v === 'default' || v === undefined) continue
        if (FIXED_COLUMNS.includes(k) || k === 'name' || k === 'description' || k === 'previewImage') { fixed[k] = v }
        else { extra[k] = v }
      }
      const payload: Record<string, unknown> = {
        name: (fixed as any).name || (allValues as any).name,
        description: (fixed as any).description || (allValues as any).description || '',
        previewImage: previewUrl,
        presetType,
        worldSize: fixed.world_size || 'default',
        branching: fixed.branching || 'default',
        loopMode: fixed.loop || 'default',
        seasonStart: fixed.season_start || 'default',
        dayMode: fixed.day || 'default',
        autumnLength: fixed.autumn || 'default',
        winterLength: fixed.winter || 'default',
        springLength: fixed.spring || 'default',
        summerLength: fixed.summer || 'default',
        resourceVariety: fixed.resourceVariety || 'default',
        extraSettings: JSON.stringify(extra),
      }
      return editing ? updateWorldGenPreset(initialValues!.id, payload) : createWorldGenPreset(payload)
    },
    successMsg: { created: t('common.templates_created'), saved: t('common.saved') },
    errorMsg: { createFailed: t('common.templates_create_failed'), updateFailed: t('common.templates_update_failed') },
  })

  useEffect(() => {
    if (open) {
      const defaults: Record<string, string> = {}
      for (const k of Object.keys(metadata)) {
        defaults[k] = DST_DEFAULTS[k] || 'default'
      }
      if (initialValues) {
        setPresetType((initialValues as any).presetType || 'surface')
        form.setFieldsValue(defaults)
        const vals: Record<string, unknown> = { ...initialValues }
        // Parse extra_settings JSON for non-fixed-column values
        if (initialValues.extraSettings) {
          try {
            const extra = typeof initialValues.extraSettings === 'string'
              ? JSON.parse(initialValues.extraSettings)
              : initialValues.extraSettings
            Object.assign(vals, extra)
          } catch { /* ignore parse errors */ }
        }
        // Map fixed column names to metadata keys
        if (initialValues.worldSize) vals.world_size = initialValues.worldSize
        if (initialValues.branching) vals.branching = initialValues.branching
        if (initialValues.loopMode) vals.loop = initialValues.loopMode
        if (initialValues.seasonStart) vals.season_start = initialValues.seasonStart
        if (initialValues.dayMode) vals.day = initialValues.dayMode
        if (initialValues.autumnLength) vals.autumn = initialValues.autumnLength
        if (initialValues.winterLength) vals.winter = initialValues.winterLength
        if (initialValues.springLength) vals.spring = initialValues.springLength
        if (initialValues.summerLength) vals.summer = initialValues.summerLength
        form.setFieldsValue(vals)
        setPreviewUrl(initialValues.previewImage || '')
        setPresetType((initialValues as any).presetType || 'surface')
      } else {
        form.setFieldsValue(defaults)
        setPreviewUrl('')
      }
    }
  }, [open, initialValues, form])

  const tVal = (v: string) => {
    const key = `templates_val_${v.replace(/-/g, '').replace(/_/g, '')}`
    const translated = t(`common.${key}`)
    return translated !== `common.${key}` ? translated : v
  }

  // Surface preset groups
  const SURFACE_GROUPS = [
    { key: 'world', title: t('common.templates_cat_world_layout'), icon: '🌍', keys: ['world_size','branching','loop','roads','task_set','start_location','spawnmode','touchstone','boons','prefabswaps_start','prefabswaps','moon_fissure','terrariumchest','stageplays','krampus'] },
    { key: 'seasons', title: t('common.templates_cat_seasons'), icon: '🕐', keys: ['season_start','day','autumn','winter','spring','summer'] },
    { key: 'resources', title: t('common.templates_cat_resources'), icon: '🪨', keys: ['flint','rock','rock_ice','trees','grass','sapling','berrybush','carrot','flowers','mushroom','reeds','cactus','tumbleweed','marshbush','ponds','gold_depth','palmconetree'] },
    { key: 'food', title: t('common.templates_cat_food'), icon: '🍌', keys: ['banana','lichen'] },
    { key: 'moon', title: t('common.templates_cat_moon'), icon: '🌙', keys: ['moon_tree','moon_rock','moon_sapling','moon_berrybush','moon_bullkelp','moon_starfish','moon_hotspring','moon_carrot','moon_fruitdragon'] },
    { key: 'ocean', title: t('common.templates_cat_ocean'), icon: '🌊', keys: ['ocean_bullkelp','ocean_seastack','ocean_waterplant','ocean_shoal','ocean_wobsterden','fishschools'] },
    { key: 'creatures', title: t('common.templates_cat_creatures'), icon: '🐝', keys: ['bees','birds','bunnymen','butterfly','catcoons','perd','grassgekkos','moles','penguins','pigs','rabbits','beefalo','beefaloheat','lightninggoat','buzzard','gnarwail','wobsters'] },
    { key: 'hostile', title: t('common.templates_cat_hostile'), icon: '💀', keys: ['spiders','cave_spiders','spider_warriors','moon_spider','mutated_hounds','hound_mounds','bats','frogs','wasps','lureplants','walrus','merms','tentacles','chess','tallbirds','mosquitos','sharks','cookiecutters','squid','penguins_moon','slurper','slurtles','rocky','monkey','worms','fissure','angrybees','pirateraids'] },
    { key: 'giants', title: t('common.templates_cat_giants'), icon: '👑', keys: ['bearger','deerclops','goosemoose','dragonfly','beequeen','klaus','crabking','toadstool','antliontribute','malbatross','eyeofterror','twins','daywalker','frostjaw','sharkboi','fruitfly','liefs','deciduousmonster','spiderqueen'] },
    { key: 'events', title: t('common.templates_cat_world_events'), icon: '⚡', keys: ['hounds','hunt','alternatehunt','wildfires','lightning','weather','frograin','meteorshowers','meteorspawner','petrification','earthquakes','winterhounds','summerhounds'] },
    { key: 'regrowth', title: t('common.templates_cat_regrowth'), icon: '🌱', keys: ['regrowth','basicresource_regrowth','carrots_regrowth','deciduoustree_regrowth','evergreen_regrowth','flowers_regrowth','moon_tree_regrowth','saltstack_regrowth','twiggytrees_regrowth','cactus_regrowth','reeds_regrowth','palmconetree_regrowth'] },
    { key: 'survivors', title: t('common.templates_cat_survivors'), icon: '👤', keys: ['extrastartingitems','seasonalstartingitems','spawnprotection','dropeverythingondespawn','healthpenalty','lessdamagetaken','temperaturedamage','hunger','darkness','shadowcreatures','brightmarecreatures','ghostenabled','portalresurection','ghostsanitydrain','resettime'] },
    { key: 'festive', title: t('common.templates_cat_festive'), icon: '🎉', keys: ['specialevent','crow_carnival','hallowed_nights','winters_feast','year_of_the_gobbler','year_of_the_varg','year_of_the_pig','year_of_the_carrat','year_of_the_beefalo','year_of_the_catcoon','year_of_the_bunnyman','year_of_the_dragon'] },
    { key: 'layout', title: t('common.templates_cat_boons'), icon: '🗺️', keys: ['has_ocean','keep_disconnected_tiles','no_joining_islands','no_wormholes_to_disconnected_tiles','layout_mode','wormhole_prefab'] },
    { key: 'nonnatural', title: t('common.templates_cat_nonnatural'), icon: '🌀', keys: ['portal_spawnrate','lightcrab_portalrate','palmcone_seed_portalrate','powder_monkey_portalrate','monkeytail_portalrate','bananabush_portalrate'] },
  ]

  // Cave preset groups — based on DST_CAVE 2026 reference
  const CAVE_GROUPS = [
    { key: 'world', title: t('common.templates_cat_world_layout'), icon: '🌍', keys: ['world_size','branching','loop','task_set','start_location','touchstone','boons','prefabswaps_start','prefabswaps','petrification','disease_delay'] },
    { key: 'day', title: t('common.templates_cat_seasons'), icon: '🕐', keys: ['day','cavelight','season_start','autumn','winter','spring','summer'] },
    { key: 'resources', title: t('common.templates_cat_resources'), icon: '🪨', keys: ['flint','rock','rock_ice','trees','grass','sapling','berrybush','carrot','flowers','mushroom','reeds','tumbleweed','marshbush'] },
    { key: 'cave_res', title: t('common.templates_cat_cave_resources'), icon: '💎', keys: ['gold_depth','mushtree','fern','flower_cave','wormlights','lichen','banana','cave_ponds'] },
    { key: 'regrowth', title: t('common.templates_cat_regrowth'), icon: '🌱', keys: ['regrowth','basicresource_regrowth','carrots_regrowth','evergreen_regrowth','flowers_regrowth','twiggytrees_regrowth'] },
    { key: 'creatures', title: t('common.templates_cat_cave_creatures'), icon: '🐰', keys: ['bunnymen','bunnymen_setting','slurper','slurtles','rocky','monkey','bats','bats_setting','moles','moles_setting','rabbits','rabbits_setting','perd','tallbirds','butterfly'] },
    { key: 'hostile', title: t('common.templates_cat_cave_hostile'), icon: '💀', keys: ['cave_spiders','spiders','spiders_setting','spider_warriors','moon_spider','worms','fissure','wormattacks','tentacles','hound_mounds','houndmound','mutated_hounds','chess','lureplants','frogs','wasps','mosquitos','krampus'] },
    { key: 'giants', title: t('common.templates_cat_giants'), icon: '👑', keys: ['bearger','deerclops','dragonfly','beequeen','antliontribute','crabking','toadstool','liefs','deciduousmonster'] },
    { key: 'events', title: t('common.templates_cat_cave_events'), icon: '⚡', keys: ['earthquakes','hounds','hunt','alternatehunt','weather','wildfires'] },
    { key: 'survivors', title: t('common.templates_cat_survivors'), icon: '👤', keys: ['extrastartingitems','seasonalstartingitems','spawnprotection','dropeverythingondespawn','healthpenalty','lessdamagetaken','temperaturedamage','hunger','darkness','shadowcreatures','brightmarecreatures','ghostenabled','portalresurection','ghostsanitydrain','resettime'] },
    { key: 'festive', title: t('common.templates_cat_festive'), icon: '🎉', keys: ['specialevent','hallowed_nights','winters_feast','year_of_the_gobbler','year_of_the_varg','year_of_the_pig','year_of_the_carrat','year_of_the_beefalo','year_of_the_catcoon','year_of_the_bunnyman'] },
  ]

  const groups = presetType === 'caves' ? CAVE_GROUPS : SURFACE_GROUPS

  // Translate a setting label: try i18n key templates_label_{key}, fallback to metadata label
  const tSettingLabel = (key: string, fallback: string) => {
    const i18nKey = `templates_label_${key}`
    const translated = t(`common.${i18nKey}`)
    // t() returns empty string for missing keys in production, or key path in dev
    if (!translated || translated === `common.${i18nKey}`) return fallback
    return translated
  }

  // Translate an option label from metadata: try i18n, fallback to tVal, fallback to raw label
  const tOptionLabel = (label: string) => {
    const slug = label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/_$/, '')
    const i18nKey = `templates_opt_${slug}`
    const translated = t(`common.${i18nKey}`)
    if (translated && translated !== `common.${i18nKey}`) return translated
    // If no i18n entry, try tVal with the label as a value-like string
    const fromVal = tVal(label.toLowerCase().replace(/\s+/g, ''))
    if (fromVal && fromVal !== label.toLowerCase().replace(/\s+/g, '')) return fromVal
    return label
  }

  const renderSettingCard = (key: string) => {
    const meta = metadata[key]
    if (!meta) return null
    const imgSrc = `/images/worldgen/${key}.png`
    return (
      <Col span={12} key={key} style={{ marginBottom: 12 }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 14,
          background: '#fff', borderRadius: 10, padding: '14px 16px',
          border: '1px solid #e8e8e8', boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          transition: 'box-shadow 0.2s',
        }}>
          <Image
            src={imgSrc}
            fallback="/images/worldgen/default.png"
            preview={false}
            width={56} height={56}
            style={{ borderRadius: 8, flexShrink: 0, border: '1px solid #f0f0f0' }}
          />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 500, color: '#333', marginBottom: 6 }}>
              {tSettingLabel(key, (meta as any).label)}
            </div>
            <Form.Item name={key} style={{ margin: 0 }} initialValue="default">
              <Select
                style={{ width: '100%' }}
                options={((meta as any).options as { value: string; label: string }[]).map((o) => ({
                  value: o.value, label: tOptionLabel(o.label),
                }))}
              />
            </Form.Item>
          </div>
        </div>
      </Col>
    )
  }

  return (
    <Modal
      title={editing ? t('common.templates_edit_preset') : t('common.templates_create_preset')}
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={900}
      destroyOnClose
      styles={{ body: { maxHeight: '70vh', overflowY: 'auto', padding: '16px 24px' } }}
    >
      <Form form={form} layout="vertical" initialValues={{ gameMode: 'survival', maxPlayers: 6 }}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={12}>
            <Form.Item name="name" label={t('common.templates_name')} rules={[{ required: true }]} style={{ marginBottom: 12 }}>
              <Input maxLength={128} placeholder={t('common.templates_name_placeholder')} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item label={t('common.templates_preset_type')} style={{ marginBottom: 12 }}>
              <Select value={presetType} onChange={(v) => setPresetType(v)} options={[
                { value: 'surface', label: t('common.templates_preset_surface') },
                { value: 'caves', label: t('common.templates_preset_caves') },
              ]} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item label={t('common.templates_cover_image')} style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <div style={{ width: 40, height: 40, borderRadius: 6, border: '1px dashed #d9d9d9', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#fafafa', flexShrink: 0, overflow: 'hidden' }}>
                  {previewUrl ? (
                    <img src={previewUrl} alt="preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }} />
                  ) : <PictureOutlined style={{ fontSize: 14, color: '#bfbfbf' }} />}
                </div>
                <Input value={previewUrl} onChange={(e) => setPreviewUrl(e.target.value)} placeholder="URL" allowClear size="small" />
              </div>
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="description" label={t('common.templates_description')} style={{ marginBottom: 0 }}>
              <Input.TextArea rows={2} maxLength={500} placeholder={t('common.templates_desc_default')} />
            </Form.Item>
          </Col>
        </Row>

        <Collapse defaultActiveKey={groups.slice(0, 4).map(c => c.key)} style={{ background: '#fff' }}>
          {groups.map((cat) => {
            const availableKeys = cat.keys.filter((k) => metadata[k])
            if (availableKeys.length === 0) return null
            return (
              <Collapse.Panel
                key={cat.key}
                header={<span>{cat.icon} {cat.title} <span style={{ color: '#888', fontSize: 12 }}>({availableKeys.length})</span></span>}
              >
                <Row gutter={8}>
                  {availableKeys.map(renderSettingCard)}
                </Row>
              </Collapse.Panel>
            )
          })}
        </Collapse>
      </Form>
    </Modal>
  )
}
