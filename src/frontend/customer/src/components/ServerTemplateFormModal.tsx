import { useEffect, useState } from 'react'
import { Modal, Form, Input, InputNumber, Select, Space, Button, Row, Col, Tag } from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import {
  createTemplate, updateTemplate, bindTemplateWorldGen,
  browseWorldGenPresets, getTemplateWorldGen,
  browseTemplates,
  type TemplateInfo, type WorldGenPresetInfo,
} from '../api/templates'
import { useTranslation } from '../i18n'
import useResourceForm from '../hooks/useResourceForm'

interface Props {
  open: boolean
  initialValues?: TemplateInfo | null
  onClose: () => void
  onSaved: () => void
}

interface BindingEntry {
  key: number
  presetId: number | undefined
  shardType: string
}

export default function ServerTemplateFormModal({ open, initialValues, onClose, onSaved }: Props) {
  const [form] = Form.useForm()
  const [allPresets, setAllPresets] = useState<WorldGenPresetInfo[]>([])
  const [bindings, setBindings] = useState<BindingEntry[]>([])
  const [modTemplates, setModTemplates] = useState<TemplateInfo[]>([])
  const [boundModIds, setBoundModIds] = useState<number[]>([])
  const { t } = useTranslation()

  const { loading, editing, handleSubmit } = useResourceForm({
    form, initialValues, onSaved, onClose,
    async onSubmit(values, isEdit): Promise<{ code: number; message?: string }> {
      const templateData = { ...values, templateType: 'server_template', modList: JSON.stringify(boundModIds) }
      const res = isEdit
        ? await updateTemplate(initialValues!.id, templateData)
        : await createTemplate(templateData)
      if (res.code === 0) {
        const templateId = res.data?.id ?? initialValues!.id
        const validBindings = bindings.filter((b) => b.presetId)
        if (validBindings.length > 0) {
          await bindTemplateWorldGen(templateId, validBindings.map((b) => ({ presetId: b.presetId!, shardType: b.shardType })))
        }
      }
      return res
    },
    successMsg: { created: t('common.templates_created'), saved: t('common.saved') },
    errorMsg: { createFailed: t('common.templates_create_failed'), updateFailed: t('common.templates_update_failed') },
  })

  useEffect(() => {
    if (open) {
      browseWorldGenPresets({ size: '200' }).then((res) => {
        if (res.code === 0) setAllPresets(res.data?.records ?? [])
      }).catch(() => {})
      browseTemplates({ type: 'modpack', size: '200' }).then((res) => {
        if (res.code === 0) setModTemplates(res.data?.records ?? [])
      }).catch(() => {})

      if (initialValues) {
        form.setFieldsValue(initialValues)
        getTemplateWorldGen(initialValues.id).then((res) => {
          if (res.code === 0 && res.data) {
            setBindings(res.data.map((p, i) => ({
              key: i, presetId: p.id, shardType: 'master',
            })))
          }
        }).catch(() => {})
      } else {
        form.resetFields()
        setBindings([])
      }
    }
  }, [open, initialValues, form])

  const addBinding = () => {
    setBindings((prev) => [...prev, { key: Date.now(), presetId: undefined, shardType: 'master' }])
  }

  const removeBinding = (key: number) => {
    setBindings((prev) => prev.filter((b) => b.key !== key))
  }

  const updateBinding = (key: number, field: 'presetId' | 'shardType', value: string | number) => {
    setBindings((prev) => prev.map((b) => (b.key === key ? { ...b, [field]: value } : b)))
  }

  const unusedPresets = allPresets.filter((p) => !bindings.find((b) => b.presetId === p.id))

  const addModTemplate = (id: number) => {
    if (!boundModIds.includes(id)) setBoundModIds((prev) => [...prev, id])
  }

  const removeModTemplate = (id: number) => {
    setBoundModIds((prev) => prev.filter((mid) => mid !== id))
  }

  return (
    <Modal
      title={editing ? t('common.templates_edit_template') : t('common.templates_create_template')}
      open={open} onCancel={onClose} onOk={handleSubmit}
      confirmLoading={loading} width={720} destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={{ gameMode: 'survival', maxPlayers: 6 }}>
        <Row gutter={16}>
          <Col span={16}>
            <Form.Item name="name" label={t('common.templates_name')} rules={[{ required: true }]}>
              <Input maxLength={128} placeholder={t('common.templates_name_placeholder')} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="maxPlayers" label={t('common.templates_max_players')}>
              <InputNumber min={1} max={64} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="category" label={t('common.templates_category')}>
              <Select allowClear options={[
                { value: 'survival', label: t('common.templates_catval_survival') },
                { value: 'pvp', label: t('common.templates_catval_pvp') },
                { value: 'caves', label: t('common.templates_catval_caves') },
                { value: 'modpack', label: t('common.templates_catval_modpack') },
                { value: 'endless', label: t('common.templates_catval_endless') },
              ]} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="gameMode" label={t('common.templates_game_mode')}>
              <Select options={[
                { value: 'survival', label: t('common.templates_catval_survival') },
                { value: 'endless', label: t('common.templates_catval_endless') },
                { value: 'wilderness', label: t('common.templates_catval_wilderness') },
              ]} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="description" label={t('common.templates_description')}>
              <Input.TextArea rows={2} maxLength={500} placeholder={t('common.templates_desc_placeholder')} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="coverImage" label={t('common.templates_cover_image')}>
              <Input placeholder={t('common.templates_cover_image_placeholder')} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="tags" label={t('common.templates_tags')}>
              <Input placeholder={t('common.templates_tags_placeholder')} />
            </Form.Item>
          </Col>
        </Row>

        <div style={{ marginTop: 16, marginBottom: 16 }}>
          <Space style={{ marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>{t('common.templates_bind_presets')}</span>
            <Button size="small" icon={<PlusOutlined />} onClick={addBinding} disabled={unusedPresets.length === 0}>
              {t('common.templates_add')}
            </Button>
          </Space>
          {bindings.length === 0 ? (
            <p style={{ color: '#888', fontSize: 13 }}>{t('common.templates_no_presets_bound')}</p>
          ) : (
            bindings.map((b) => (
              <Row gutter={8} key={b.key} style={{ marginBottom: 8 }}>
                <Col span={14}>
                  <Select
                    value={b.presetId} onChange={(v) => updateBinding(b.key, 'presetId', v)}
                    placeholder={t('common.templates_select_preset')} style={{ width: '100%' }}
                    options={[
                      ...(b.presetId ? allPresets.filter((p) => p.id === b.presetId).map((p) => ({ value: p.id, label: p.name })) : []),
                      ...unusedPresets.map((p) => ({ value: p.id, label: p.name })),
                    ]}
                  />
                </Col>
                <Col span={6}>
                  <Select
                    value={b.shardType} onChange={(v) => updateBinding(b.key, 'shardType', v)}
                    options={[
                      { value: 'master', label: t('common.templates_cat_master') },
                      { value: 'caves', label: t('common.templates_cat_caves') },
                    ]}
                  />
                </Col>
                <Col span={4}>
                  <Button danger icon={<DeleteOutlined />} onClick={() => removeBinding(b.key)} />
                </Col>
              </Row>
            ))
          )}
        </div>

        {/* Mod Template Binding sub-form */}
        <div style={{ marginTop: 16, marginBottom: 16 }}>
          <Space style={{ marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>{t('common.templates_bound_mods')}</span>
          </Space>
          <Select
            onChange={(id) => { if (typeof id === 'number') addModTemplate(id) }}
            placeholder={t('common.templates_select_mod_template')}
            style={{ width: '100%', marginBottom: 8 }}
            options={modTemplates.filter((mt) => !boundModIds.includes(mt.id)).map((mt) => ({
              value: mt.id,
              label: `${mt.name} (${mt.category || 'modpack'})`,
            }))}
          />
          {boundModIds.length === 0 ? (
            <p style={{ color: '#888', fontSize: 13 }}>{t('common.templates_no_mods_bound')}</p>
          ) : (
            boundModIds.map((id) => {
              const mt = modTemplates.find((m) => m.id === id)
              return (
                <Tag key={id} closable onClose={() => removeModTemplate(id)} style={{ marginBottom: 4 }}>
                  {mt?.name || `Template #${id}`}
                </Tag>
              )
            })
          )}
        </div>
      </Form>
    </Modal>
  )
}
