import { Button } from 'antd'
import useCountdown from '../hooks/useCountdown'
import { TIMEOUTS } from '../utils/constants'
import { useTranslation } from '../i18n'

interface CountdownButtonProps {
  onClick: () => void | Promise<void>
  cooldownSeconds?: number
  children: React.ReactNode
  loading?: boolean
  disabled?: boolean
  block?: boolean
}

export default function CountdownButton({
  onClick,
  cooldownSeconds = TIMEOUTS.CODE_RESEND_COOLDOWN,
  children,
  loading = false,
  disabled = false,
  block = false,
}: CountdownButtonProps) {
  const { count, isRunning, start } = useCountdown(cooldownSeconds)
  const { t } = useTranslation()

  const handleClick = async () => {
    await onClick()
    start()
  }

  if (isRunning) {
    return (
      <Button disabled block={block}>
        {t('auth.resend_in', { seconds: count })}
      </Button>
    )
  }

  return (
    <Button
      onClick={handleClick}
      loading={loading}
      disabled={disabled}
      block={block}
    >
      {children}
    </Button>
  )
}
