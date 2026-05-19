import { Button, Dropdown } from 'antd'
import { GlobalOutlined } from '@ant-design/icons'
import { useTranslation } from '../i18n'
import { SUPPORTED_LOCALES } from '../i18n/config'

export default function LanguageSwitcher() {
  const { locale, setLocale } = useTranslation()

  const currentLabel = SUPPORTED_LOCALES.find((l) => l.code === locale)?.label || locale

  const items = SUPPORTED_LOCALES.map((l) => ({
    key: l.code,
    label: l.label,
    onClick: () => setLocale(l.code),
    disabled: l.code === locale,
  }))

  return (
    <div style={{ textAlign: 'right', marginBottom: 8 }}>
      <Dropdown menu={{ items }} trigger={['click']}>
        <Button
          type="text"
          size="small"
          icon={<GlobalOutlined />}
          aria-label={`Current language: ${currentLabel}. Click to change.`}
        >
          {currentLabel}
        </Button>
      </Dropdown>
    </div>
  )
}
