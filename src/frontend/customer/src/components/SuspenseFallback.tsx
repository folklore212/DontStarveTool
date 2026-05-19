import { Spin } from 'antd'
import { useTranslation } from '../i18n'

export default function SuspenseFallback() {
  const { t } = useTranslation()
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: 200,
      }}
      role="status"
      aria-live="polite"
      aria-label={t('common.loading')}
    >
      <Spin size="large" />
    </div>
  )
}
