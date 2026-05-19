import { useState, useEffect, useRef } from 'react'
import { Modal, Input, List, Typography } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'

interface Command { key: string; label: string; description: string; action: () => void }

export default function CommandPalette() {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const navigate = useNavigate()
  const inputRef = useRef<any>(null)

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setOpen((v) => !v)
        setQuery('')
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])

  useEffect(() => { if (open) setTimeout(() => inputRef.current?.focus(), 100) }, [open])

  const allCommands: Command[] = [
    { key: 'dashboard', label: 'Dashboard', description: 'Go to Dashboard', action: () => navigate('/dashboard') },
    { key: 'servers', label: 'Servers', description: 'Manage remote servers', action: () => navigate('/servers') },
    { key: 'deploy', label: 'Deploy Wizard', description: 'Deploy a DST world', action: () => navigate('/servers/deploy') },
    { key: 'marketplace', label: 'Marketplace', description: 'Browse world configs', action: () => navigate('/marketplace') },
    { key: 'analytics', label: 'Analytics', description: 'View analytics dashboard', action: () => navigate('/analytics') },
    { key: 'profile', label: 'Profile', description: 'Edit your profile', action: () => navigate('/profile') },
    { key: 'security', label: 'Security', description: 'Security settings', action: () => navigate('/security') },
  ]

  const filtered = query ? allCommands.filter((c) => c.label.toLowerCase().includes(query.toLowerCase())) : allCommands.slice(0, 5)

  return (
    <Modal open={open} onCancel={() => setOpen(false)} footer={null} closable={false} width={560} styles={{ body: { padding: 0 } }}>
      <Input ref={inputRef} prefix={<SearchOutlined />} placeholder="Type a command..." value={query}
        onChange={(e) => setQuery(e.target.value)} size="large" bordered={false} style={{ padding: '12px 16px', fontSize: 16 }} />
      <List dataSource={filtered} renderItem={(c) => (
        <List.Item onClick={() => { c.action(); setOpen(false) }} style={{ cursor: 'pointer', padding: '10px 16px' }}>
          <div>
            <Typography.Text strong>{c.label}</Typography.Text><br />
            <Typography.Text type="secondary">{c.description}</Typography.Text>
          </div>
        </List.Item>
      )} />
      <div style={{ padding: '8px 16px', borderTop: '1px solid #f0f0f0' }}>
        <Typography.Text type="secondary" style={{ fontSize: 11 }}>Press ⌘K to toggle · Esc to close</Typography.Text>
      </div>
    </Modal>
  )
}
