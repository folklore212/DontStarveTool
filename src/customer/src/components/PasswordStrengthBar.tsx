import usePasswordStrength from '../hooks/usePasswordStrength'
import { useTranslation } from '../i18n'

const SEGMENTS = [1, 2, 3, 4] as const

interface PasswordStrengthBarProps {
  password: string
  username?: string
  email?: string
}

export default function PasswordStrengthBar({
  password,
  username,
  email,
}: PasswordStrengthBarProps) {
  const { score, label, color } = usePasswordStrength(password, username, email)
  const { t } = useTranslation()

  return (
    <div style={{ marginTop: 4, marginBottom: 8 }}>
      <div className="password-strength-bar" role="progressbar" aria-valuenow={score} aria-valuemin={0} aria-valuemax={4} aria-label={t(label)}>
        {SEGMENTS.map((segment) => (
          <div
            key={segment}
            className="password-strength-segment"
            style={{
              backgroundColor: segment <= score ? color : '#e8e8e8',
            }}
          />
        ))}
      </div>
      <div style={{ fontSize: 12, color, marginTop: 4, minHeight: 18 }}>
        {password ? t(label) : ''}
      </div>
    </div>
  )
}
