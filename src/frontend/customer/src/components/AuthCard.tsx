import type { ReactNode } from 'react'
import { Card } from 'antd'

interface AuthCardProps {
  title?: ReactNode
  subtitle?: ReactNode
  children: ReactNode
  maxWidth?: number
  className?: string
}

export default function AuthCard({
  title,
  subtitle,
  children,
  maxWidth = 400,
  className,
}: AuthCardProps) {
  return (
    <div className="auth-container">
      <Card className={`auth-card ${className || ''}`} style={{ maxWidth }}>
        {title && <div className="auth-title">{title}</div>}
        {subtitle && <div className="auth-subtitle">{subtitle}</div>}
        {children}
      </Card>
    </div>
  )
}
