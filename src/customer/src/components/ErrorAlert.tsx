import { Alert, Button, Space } from 'antd'
import { useEffect, useState } from 'react'
import { useTranslation } from '../i18n'
import { TIMEOUTS } from '../utils/constants'

interface ErrorAlertProps {
  message: string
  type?: 'error' | 'warning' | 'info'
  closable?: boolean
  onClose?: () => void
  retryAction?: () => void
  autoDismiss?: boolean
}

export default function ErrorAlert({
  message,
  type = 'error',
  closable = true,
  onClose,
  retryAction,
  autoDismiss = false,
}: ErrorAlertProps) {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(true)
  const [isHovered, setIsHovered] = useState(false)

  useEffect(() => {
    if (!autoDismiss || isHovered) return

    const timer = setTimeout(() => {
      setVisible(false)
      onClose?.()
    }, TIMEOUTS.ERROR_AUTO_DISMISS)

    return () => clearTimeout(timer)
  }, [autoDismiss, isHovered, onClose])

  if (!visible || !message) return null

  return (
    <Alert
      type={type}
      message={message}
      closable={closable}
      style={{ marginBottom: 16 }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onClose={() => {
        setVisible(false)
        onClose?.()
      }}
      action={
        retryAction ? (
          <Space>
            <Button size="small" onClick={retryAction}>
              {t('common.retry')}
            </Button>
          </Space>
        ) : undefined
      }
    />
  )
}
