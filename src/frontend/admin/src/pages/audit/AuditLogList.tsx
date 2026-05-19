import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Tag,
  Typography,
  Modal,
  Descriptions,
  message,
} from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { AxiosError } from 'axios'
import type { ColumnsType } from 'antd/es/table'
import type { AuditLogVO } from '../../types'
import * as auditApi from '../../api/audit'

const { Title } = Typography

function AuditLogList() {
  const [logs, setLogs] = useState<AuditLogVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [filters, setFilters] = useState<Record<string, string>>({})
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedLog, setSelectedLog] = useState<AuditLogVO | null>(null)

  const fetchLogs = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { page, size, ...filters }
      if (filters.userId) params.userId = Number(filters.userId)
      const res = await auditApi.queryAuditLogs(params)
      const d = res.data.data
      setLogs(d?.records ?? [])
      setTotal(d?.total ?? 0)
    } catch (err: unknown) {
      const status = (err as AxiosError)?.response?.status
      if (status === 403) {
        message.warning('Insufficient permissions to view audit logs')
      } else {
        message.error('Failed to load audit logs')
      }
    } finally {
      setLoading(false)
    }
  }, [page, size, filters])

  useEffect(() => { fetchLogs() }, [fetchLogs])

  const columns: ColumnsType<AuditLogVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'User ID', dataIndex: 'userId', key: 'userId', width: 80 },
    { title: 'Action', dataIndex: 'action', key: 'action', width: 130, render: (v) => <Tag>{v}</Tag> },
    { title: 'Resource', key: 'resource', width: 160, render: (_: unknown, r: AuditLogVO) =>
      `${r.resourceType}${r.resourceId ? `:${r.resourceId}` : ''}`
    },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 130, render: (v) => v || '-' },
    { title: 'Session ID', dataIndex: 'sessionId', key: 'sessionId', width: 140, ellipsis: true, render: (v) => v || '-' },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
    {
      title: 'Detail', key: 'detail', width: 80,
      render: (_: unknown, record: AuditLogVO) => (
        <Button size="small" onClick={() => { setSelectedLog(record); setDetailOpen(true) }}>
          View
        </Button>
      ),
    },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>Audit Logs</Title>
        <Button icon={<ReloadOutlined />} onClick={fetchLogs}>Refresh</Button>
      </div>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="User ID"
          style={{ width: 130 }}
          onSearch={(v) => setFilters({ ...filters, userId: v })}
          allowClear
        />
        <Input.Search
          placeholder="Action"
          style={{ width: 150 }}
          onSearch={(v) => setFilters({ ...filters, action: v })}
          allowClear
        />
        <Input.Search
          placeholder="Resource Type"
          style={{ width: 160 }}
          onSearch={(v) => setFilters({ ...filters, resourceType: v })}
          allowClear
        />
        <Button onClick={() => { setFilters({}); setPage(1) }}>Clear Filters</Button>
      </Space>

      <Table
        dataSource={logs}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `Total ${t}`,
          onChange: (p, s) => { setPage(p); setSize(s) },
        }}
      />

      <Modal
        title="Audit Log Detail"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={<Button onClick={() => setDetailOpen(false)}>Close</Button>}
        width={600}
      >
        {selectedLog && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{selectedLog.id}</Descriptions.Item>
            <Descriptions.Item label="User ID">{selectedLog.userId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Client ID">{selectedLog.clientId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Action">{selectedLog.action}</Descriptions.Item>
            <Descriptions.Item label="Resource">{selectedLog.resourceType}:{selectedLog.resourceId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="IP Address">{selectedLog.ipAddress ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Session ID">{selectedLog.sessionId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Request ID">{selectedLog.requestId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Client IP Chain">{selectedLog.clientIpChain ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Created">{selectedLog.createdAt}</Descriptions.Item>
            <Descriptions.Item label="Detail">{selectedLog.detail ?? '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </Card>
  )
}

export default AuditLogList
