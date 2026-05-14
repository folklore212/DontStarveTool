import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, message, Row, Col, Divider } from 'antd'
import {
  GlobalOutlined, BranchesOutlined, PartitionOutlined,
  FieldTimeOutlined, SunOutlined, ExperimentOutlined,
  PictureOutlined, BugOutlined, GiftOutlined,
  CloudOutlined, SyncOutlined, ToolOutlined,
} from '@ant-design/icons'
import {
  createWorldGenPreset, updateWorldGenPreset,
  getWorldGenMetadata, type WorldGenPresetInfo, type WorldGenMetadata,
} from '../api/templates'
import { useTranslation } from '../i18n'

const SETTING_ICONS: Record<string, React.ReactNode> = {
  worldSize: <GlobalOutlined />, branching: <BranchesOutlined />,
  loopMode: <PartitionOutlined />, seasonStart: <FieldTimeOutlined />,
  dayMode: <SunOutlined />, resourceVariety: <ExperimentOutlined />,
  creatures: <BugOutlined />, boons: <GiftOutlined />,
  weather: <CloudOutlined />, regrowth: <SyncOutlined />,
  startingGear: <ToolOutlined />,
}

const FIELD_DEFAULTS: Record<string, string> = {
  worldSize: 'default', branching: 'default', loopMode: 'default',
  seasonStart: 'default', dayMode: 'default',
  autumnLength: 'default', winterLength: 'default', springLength: 'default',
  summerLength: 'default', resourceVariety: 'default',
  creatures: 'default', boons: 'default', weather: 'default',
  regrowth: 'default', startingGear: 'default',
}

const SETTING_GROUPS = [
  {
    title: 'World Layout',
    icon: <GlobalOutlined />,
    keys: ['worldSize', 'branching', 'loopMode'],
  },
  {
    title: 'Seasons & Day Cycle',
    icon: <FieldTimeOutlined />,
    keys: ['seasonStart', 'dayMode', 'autumnLength', 'winterLength', 'springLength', 'summerLength'],
  },
  {
    title: 'Resources & Regrowth',
    icon: <ExperimentOutlined />,
    keys: ['resourceVariety', 'regrowth', 'startingGear'],
  },
  {
    title: 'Creatures & World Features',
    icon: <BugOutlined />,
    keys: ['creatures', 'boons', 'weather'],
  },
]

interface Props {
  open: boolean
  initialValues?: WorldGenPresetInfo | null
  onClose: () => void
  onSaved: () => void
}

export default function WorldGenPresetFormModal({ open, initialValues, onClose, onSaved }: Props) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [metadata, setMetadata] = useState<WorldGenMetadata>({})
  const [previewUrl, setPreviewUrl] = useState('')
  const { t } = useTranslation()
  const editing = !!initialValues

  useEffect(() => {
    if (open) {
      getWorldGenMetadata().then((res) => {
        if (res.code === 0) setMetadata(res.data ?? {})
      }).catch(() => {})
      if (initialValues) {
        form.setFieldsValue({ ...FIELD_DEFAULTS, ...initialValues })
        setPreviewUrl(initialValues.previewImage || '')
      } else {
        form.setFieldsValue(FIELD_DEFAULTS)
        setPreviewUrl('')
      }
    }
  }, [open, initialValues, form])

  const handleSubmit = async () => {
    setLoading(true)
    try {
      const values = await form.validateFields()
      values.previewImage = previewUrl
      const res = editing
        ? await updateWorldGenPreset(initialValues!.id, values)
        : await createWorldGenPreset(values)
      if (res.code === 0) {
        message.success(editing ? t('common.saved') : 'Created!')
        onSaved()
        onClose()
      } else {
        message.error(res.message)
      }
    } catch {
      message.error(editing ? 'Update failed' : 'Create failed')
    } finally {
      setLoading(false)
    }
  }

  const tVal = (v: string) => {
    const key = `templates_val_${v.replace(/-/g, '')}`
    const translated = t(`common.${key}`)
    return translated !== `common.${key}` ? translated : v
  }

  const tLabel = (key: string) => {
    const labelKey = `templates_${key.replace(/([A-Z])/g, '_$1').toLowerCase()}`
    return t(`common.${labelKey}`)
  }

  const renderSettingField = (key: string) => {
    const meta = metadata[key]
    if (!meta) return null
    return (
      <Col span={12} key={key}>
        <Form.Item name={key} label={<>{SETTING_ICONS[key]} {tLabel(key)}</>}>
          <Select
            options={(meta as { options: { value: string; label: string }[] }).options.map((o) => ({
              value: o.value,
              label: tVal(o.value),
            }))}
          />
        </Form.Item>
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
      width={760}
      destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={FIELD_DEFAULTS}>
        <Row gutter={16}>
          <Col span={14}>
            <Form.Item name="name" label="Name" rules={[{ required: true }]}>
              <Input maxLength={128} placeholder="e.g. Mega Base Builder" />
            </Form.Item>
          </Col>
          <Col span={10}>
            <Form.Item label="Preview">
              <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                <div style={{
                  width: 64, height: 64, borderRadius: 8, border: '1px dashed #d9d9d9',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  background: '#fafafa', flexShrink: 0, overflow: 'hidden',
                }}>
                  {previewUrl ? (
                    <img src={previewUrl} alt="preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }} />
                  ) : (
                    <div style={{ textAlign: 'center', color: '#bfbfbf' }}>
                      <PictureOutlined style={{ fontSize: 20 }} />
                    </div>
                  )}
                </div>
                <Input
                  value={previewUrl}
                  onChange={(e) => setPreviewUrl(e.target.value)}
                  placeholder="Image URL (optional)"
                  allowClear
                  style={{ flex: 1 }}
                />
              </div>
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="description" label={t('common.templates_description')}>
              <Input.TextArea rows={2} maxLength={500} placeholder={t('common.templates_desc_default')} />
            </Form.Item>
          </Col>
        </Row>

        {SETTING_GROUPS.map((group) => (
          <div key={group.title}>
            <Divider orientation="left" style={{ fontSize: 13, color: '#888', fontWeight: 500, textTransform: 'uppercase', marginTop: 8, marginBottom: 12 }}>
              {group.icon} {group.title}
            </Divider>
            <Row gutter={16}>
              {group.keys.map(renderSettingField)}
            </Row>
          </div>
        ))}
      </Form>
    </Modal>
  )
}
