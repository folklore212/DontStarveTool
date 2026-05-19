import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, Button, message, Row, Col, Tag, Progress, Switch, Tooltip } from 'antd'
import { DeleteOutlined, ReloadOutlined, DownloadOutlined, SettingOutlined } from '@ant-design/icons'
import { createTemplate, updateTemplate, searchWorkshopMods, type TemplateInfo, type WorkshopModInfo } from '../api/templates'
import client from '../api/client'
import { useTranslation } from '../i18n'

interface ModEntry {
  workshopId: string
  title: string
  configStatus: 'unknown' | 'loading' | 'loaded' | 'error'
  configOptions?: ModConfigOption[]
  configValues?: Record<string, unknown>
  version?: string
}

interface ModConfigOption {
  name: string
  label: string
  default: string | number | boolean
  options?: { description: string; data: string | number }[]
}

interface Props {
  open: boolean
  initialValues?: TemplateInfo | null
  onClose: () => void
  onSaved: () => void
}

export default function ModpackTemplateFormModal({ open, initialValues, onClose, onSaved }: Props) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [searchResults, setSearchResults] = useState<WorkshopModInfo[]>([])
  const [mods, setMods] = useState<ModEntry[]>([])
  const { t } = useTranslation()
  const editing = !!initialValues

  useEffect(() => {
    if (open) {
      if (initialValues) {
        form.setFieldsValue(initialValues)
        // Parse modList + configJson to restore mods
        try {
          const configJson = initialValues.configJson ? JSON.parse(initialValues.configJson) : {}
          const modList = initialValues.modList ? JSON.parse(initialValues.modList) : []
          setMods(modList.map((wid: string) => ({
            workshopId: wid,
            title: `Workshop ${wid}`,
            configStatus: configJson[`workshop-${wid}`] ? 'loaded' as const : 'unknown' as const,
            configOptions: configJson[`workshop-${wid}`]?.configuration_options,
            configValues: configJson[`workshop-${wid}`]?.configuration_options_values,
            version: configJson[`workshop-${wid}`]?.version,
          })))
        } catch { setMods([]) }
      } else {
        form.resetFields()
        setMods([])
      }
    }
  }, [open, initialValues, form])

  const searchMods = async (keyword: string) => {
    if (!keyword || keyword.length < 2) { setSearchResults([]); return }
    try {
      const res = await searchWorkshopMods(keyword)
      if (res.code === 0) setSearchResults((res.data ?? []).filter((m) => !mods.find((mod) => mod.workshopId === m.workshopId)))
    } catch { /* silent */ }
  }

  const addMod = (mod: WorkshopModInfo) => {
    setMods((prev) => [...prev, { workshopId: mod.workshopId, title: mod.title, configStatus: 'unknown' }])
    setSearchResults([])
  }

  const removeMod = (workshopId: string) => {
    setMods((prev) => prev.filter((m) => m.workshopId !== workshopId))
  }

  const fetchModConfig = async (workshopId: string) => {
    setMods((prev) => prev.map((m) => m.workshopId === workshopId ? { ...m, configStatus: 'loading' } : m))
    try {
      const res = await client.get(`/workshop/config/${workshopId}`)
      if (res.data?.code === 0 && res.data?.data) {
        const data = res.data.data
        setMods((prev) => prev.map((m) => m.workshopId === workshopId ? {
          ...m, configStatus: 'loaded',
          configOptions: data.config?.configuration_options || data.config,
          version: data.version,
        } : m))
      } else {
        setMods((prev) => prev.map((m) => m.workshopId === workshopId ? { ...m, configStatus: 'error' } : m))
      }
    } catch {
      setMods((prev) => prev.map((m) => m.workshopId === workshopId ? { ...m, configStatus: 'error' } : m))
    }
  }

  const handleSubmit = async () => {
    setLoading(true)
    try {
      const values = await form.validateFields()
      // Build configJson from mods
      const configObj: Record<string, unknown> = {}
      mods.forEach((m) => {
        configObj[`workshop-${m.workshopId}`] = {
          enabled: true,
          version: m.version,
          configuration_options: m.configValues || {},
        }
      })
      const templateData = {
        ...values, templateType: 'modpack',
        modList: JSON.stringify(mods.map((m) => m.workshopId)),
        configJson: JSON.stringify(configObj),
      }
      const res = editing
        ? await updateTemplate(initialValues!.id, templateData)
        : await createTemplate(templateData)
      if (res.code === 0) { message.success(editing ? t('common.saved') : t('common.templates_created')); onSaved(); onClose() }
      else message.error(res.message)
    } catch { message.error(editing ? t('common.templates_update_failed') : t('common.templates_create_failed')) }
    finally { setLoading(false) }
  }

  return (
    <Modal title={editing ? 'Edit Mod Template' : 'Create Mod Template'} open={open} onCancel={onClose} onOk={handleSubmit}
      confirmLoading={loading} width={800} destroyOnClose styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}>
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={14}>
            <Form.Item name="name" label={t('common.templates_name')} rules={[{ required: true }]}>
              <Input maxLength={128} placeholder={t('common.templates_name_placeholder')} />
            </Form.Item>
          </Col>
          <Col span={10}>
            <Form.Item name="category" label={t('common.templates_category')}>
              <Select allowClear options={[
                { value: 'modpack', label: t('common.templates_catval_modpack') },
                { value: 'survival', label: t('common.templates_catval_survival') },
              ]} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="description" label={t('common.templates_description')}>
              <Input.TextArea rows={2} maxLength={500} placeholder={t('common.templates_desc_default')} />
            </Form.Item>
          </Col>
        </Row>

        {/* Mod Search */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 500, marginBottom: 8 }}>{t('common.templates_bound_mods')}</div>
          <Select
            showSearch
            value={undefined}
            placeholder={t('common.templates_search_mods')}
            filterOption={false}
            onSearch={searchMods}
            onChange={(id) => {
              const mod = searchResults.find((m) => m.workshopId === id)
              if (mod) addMod(mod)
            }}
            style={{ width: '100%', marginBottom: 8 }}
            options={searchResults.map((m) => ({
              value: m.workshopId,
              label: `${m.title} (⭐ ${(m.subscriptions || 0).toLocaleString()})`,
            }))}
          />
          {mods.length === 0 ? (
            <p style={{ color: '#888', fontSize: 13 }}>{t('common.templates_no_mods_bound')}</p>
          ) : (
            mods.map((mod) => (
              <div key={mod.workshopId} style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: 12, marginBottom: 8, background: '#fafafa' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <div>
                    <strong>{mod.title}</strong>
                    <Tag style={{ marginLeft: 8 }}>{mod.workshopId}</Tag>
                    {mod.version && <Tag color="blue">v{mod.version}</Tag>}
                    {mod.configStatus === 'loaded' && <Tag color="green"><SettingOutlined /> Configured</Tag>}
                    {mod.configStatus === 'error' && <Tag color="red">Failed</Tag>}
                  </div>
                  <div>
                    {(mod.configStatus === 'unknown' || mod.configStatus === 'error') && (
                      <Tooltip title="Fetch mod configuration options">
                        <Button size="small" icon={<DownloadOutlined />} onClick={() => fetchModConfig(mod.workshopId)} loading={mod.configStatus === ('loading' as any)}>
                          Fetch Config
                        </Button>
                      </Tooltip>
                    )}
                    {mod.configStatus === 'loaded' && (
                      <Tooltip title="Refresh configuration">
                        <Button size="small" icon={<ReloadOutlined />} onClick={() => fetchModConfig(mod.workshopId)}>Refresh</Button>
                      </Tooltip>
                    )}
                    <Button size="small" danger icon={<DeleteOutlined />} onClick={() => removeMod(mod.workshopId)} style={{ marginLeft: 8 }} />
                  </div>
                </div>
                {mod.configStatus === 'loading' && <Progress percent={50} status="active" size="small" />}
                {mod.configStatus === 'loaded' && mod.configOptions && mod.configOptions.length > 0 && (
                  <div style={{ paddingLeft: 16, borderLeft: '2px solid #1677ff' }}>
                    {mod.configOptions.map((opt) => (
                      <div key={opt.name} style={{ marginBottom: 8 }}>
                        <div style={{ fontSize: 12, color: '#888', marginBottom: 2 }}>{opt.label || opt.name}</div>
                        {opt.options && opt.options.length > 0 ? (
                          <Select
                            size="small"
                            style={{ width: '100%' }}
                            defaultValue={opt.default}
                            onChange={(v) => {
                              setMods((prev) => prev.map((m) => m.workshopId === mod.workshopId ? {
                                ...m, configValues: { ...m.configValues, [opt.name]: v },
                              } : m))
                            }}
                            options={opt.options.map((o) => ({ value: o.data, label: o.description }))}
                          />
                        ) : typeof opt.default === 'boolean' ? (
                          <Switch size="small" defaultChecked={opt.default} onChange={(v) => {
                            setMods((prev) => prev.map((m) => m.workshopId === mod.workshopId ? {
                              ...m, configValues: { ...m.configValues, [opt.name]: v },
                            } : m))
                          }} />
                        ) : (
                          <Input size="small" defaultValue={String(opt.default)} onChange={(e) => {
                            setMods((prev) => prev.map((m) => m.workshopId === mod.workshopId ? {
                              ...m, configValues: { ...m.configValues, [opt.name]: e.target.value },
                            } : m))
                          }} />
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </Form>
    </Modal>
  )
}
