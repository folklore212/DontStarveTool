import { Spin } from 'antd'
import { useTranslation } from '../i18n'

interface LoadingOverlayProps {
  tip?: string
}

export default function LoadingOverlay({ tip }: LoadingOverlayProps) {
  const { t } = useTranslation()
  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'rgba(255, 255, 255, 0.65)',
        zIndex: 1000,
      }}
      role="status"
      aria-live="polite"
      aria-label={tip || t('common.loading')}
    >
      <Spin size="large" tip={tip}>
        {/* Spin requires children for tip to show */}
        <div style={{ padding: 50 }} />
      </Spin>
    </div>
  )
}
