import { Skeleton, Empty, Row, Col } from 'antd'
import type { ReactNode } from 'react'

interface Props {
  loading: boolean
  isEmpty: boolean
  emptyDescription?: string
  skeletonRows?: number
  children: ReactNode
}

export default function AsyncContentView({ loading, isEmpty, emptyDescription, skeletonRows = 4, children }: Props) {
  if (loading) return <Skeleton active paragraph={{ rows: skeletonRows }} />
  if (isEmpty) return <Empty description={emptyDescription} style={{ padding: 40 }} />
  return <>{children}</>
}

export function CardGrid({ children }: { children: ReactNode }) {
  return <Row gutter={[16, 16]}>{children}</Row>
}

export function CardGridItem({ children }: { children: ReactNode }) {
  return <Col xs={24} sm={12} lg={8} xl={6}>{children}</Col>
}
