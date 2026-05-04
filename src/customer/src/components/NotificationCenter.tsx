import { useState } from 'react'
import { Badge, Popover, List, Button, Empty, Tabs, Typography } from 'antd'
import { BellOutlined } from '@ant-design/icons'

interface Notice { id: string; title: string; body: string; level: string; time: string }

export default function NotificationCenter() {
  const [notices] = useState<Notice[]>([])
  const unreadCount = notices.length

  const content = (
    <div style={{ width: 360 }}>
      <Tabs items={[
        { key: 'all', label: `All (${unreadCount})`, children: (
          notices.length === 0 ? <Empty description="No notifications" /> : (
            <List dataSource={notices} renderItem={(n) => (
              <List.Item>
                <div>
                  <Typography.Text strong>{n.title}</Typography.Text><br />
                  <Typography.Text type="secondary">{n.body}</Typography.Text><br />
                  <Typography.Text type="secondary" style={{ fontSize: 11 }}>{n.time}</Typography.Text>
                </div>
              </List.Item>
            )} />
          )
        )},
      ]} />
    </div>
  )

  return (
    <Popover content={content} title="Notifications" trigger="click" placement="bottomRight">
      <Badge count={unreadCount} size="small">
        <Button type="text" icon={<BellOutlined />} />
      </Badge>
    </Popover>
  )
}
