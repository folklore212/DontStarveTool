import { Card, Col, Tag, Space, Rate, Tooltip, Image } from 'antd'
import { EyeOutlined, ForkOutlined, RocketOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'

interface CardAction {
  key: string
  icon: ReactNode
  label?: string
  onClick: () => void
  danger?: boolean
  highlighted?: boolean
}

interface Props {
  title: string
  description?: string
  coverImage?: string
  coverGradient: string
  coverIcon?: ReactNode
  tags: { label: string; color?: string }[]
  rating?: { avg: number; count: number }
  verified?: boolean
  verifiedLabel?: string
  extra?: ReactNode
  actions: CardAction[]
  onViewDetail?: () => void
  onFork?: () => void
  forkCount?: number
  onDeploy?: () => void
  deployLabel?: string
}

export default function ResourceCard({
  title, description = 'No description', coverImage, coverGradient, coverIcon,
  tags, rating, verified, verifiedLabel = 'VERIFIED', extra, actions,
  onViewDetail, onFork, forkCount, onDeploy, deployLabel,
}: Props) {
  const builtInActions: CardAction[] = [
    ...(onViewDetail ? [{ key: 'view', icon: <EyeOutlined />, label: 'view', onClick: onViewDetail, danger: false }] : []),
    ...(onFork !== undefined ? [{ key: 'fork', icon: <ForkOutlined />, label: `Fork (${forkCount || 0})`, onClick: onFork, danger: false }] : []),
    ...(onDeploy ? [{ key: 'deploy', icon: <RocketOutlined />, label: deployLabel || 'deploy', onClick: onDeploy, danger: false }] : []),
    ...actions,
  ]

  return (
    <Col xs={24} sm={12} lg={8} xl={6}>
      <Card
        hoverable
        style={{ borderRadius: 12, overflow: 'hidden', border: '1px solid #f0f0f0', transition: 'all .25s' }}
        cover={
          coverImage ? (
            <div style={{ height: 150, overflow: 'hidden', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Image src={coverImage} alt={title} style={{ objectFit: 'cover', width: '100%', height: '100%' }} fallback="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjYmZiZmJmIiBmb250LXNpemU9IjIwIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=" />
            </div>
          ) : (
            <div style={{ height: 150, background: coverGradient, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {coverIcon || null}
            </div>
          )
        }
        actions={builtInActions.map((a) => (
          <Tooltip key={a.key} title={a.label || a.key}>
            <span onClick={a.onClick} style={{ color: a.danger ? '#ff4d4f' : a.highlighted ? '#faad14' : undefined }}>
              {a.icon}
            </span>
          </Tooltip>
        ))}
      >
        <Card.Meta
          title={
            <Space size={4}>
              {title}
              {verified && <Tag color="gold" style={{ fontSize: 10, lineHeight: '16px' }}>{verifiedLabel}</Tag>}
            </Space>
          }
          description={
            <>
              <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 8, lineHeight: '18px', height: 36, overflow: 'hidden' }}>
                {description}
              </div>
              <Space wrap size={[0, 4]}>
                {tags.map((t, i) => <Tag key={i} color={t.color || 'blue'}>{t.label}</Tag>)}
              </Space>
              {rating && (
                <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Rate disabled value={Math.round(rating.avg || 0)} style={{ fontSize: 12 }} />
                  <span style={{ color: '#8c8c8c', fontSize: 12 }}>({rating.count || 0})</span>
                </div>
              )}
              {extra}
            </>
          }
        />
      </Card>
    </Col>
  )
}
