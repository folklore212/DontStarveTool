import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Tag,
  Typography,
  Select,
  message,
} from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { AxiosError } from 'axios'
import type { ColumnsType } from 'antd/es/table'
import type { LoginLogVO } from '../../types'
import * as auditApi from '../../api/audit'

const { Title } = Typography

const resultColors: Record<string, string> = {
  success: 'green',
  failed_credential: 'red',
  failed_mfa: 'red',
  failed_locked: 'orange',
  failed_disabled: 'orange',
}

function LoginLogList() {
  const [logs, setLogs] = useState<LoginLogVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [filters, setFilters] = useState<Record<string, string | undefined>>({})

  const fetchLogs = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { page, size }
      if (filters.userId) params.userId = Number(filters.userId)
      if (filters.result) params.result = filters.result
      if (filters.identityType) params.identityType = filters.identityType
      const res = await auditApi.queryLoginLogs(params)
      const d = res.data.data
      setLogs(d?.records ?? [])
      setTotal(d?.total ?? 0)
    } catch (err: unknown) {
      const status = (err as AxiosError)?.response?.status
      if (status === 403) {
        message.warning('Insufficient permissions to view login logs')
      } else {
        message.error('Failed to load login logs')
      }
    } finally {
      setLoading(false)
    }
  }, [page, size, filters])

  useEffect(() => { fetchLogs() }, [fetchLogs])

  const columns: ColumnsType<LoginLogVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'User ID', dataIndex: 'userId', key: 'userId', width: 80 },
    { title: 'Identifier Hash', dataIndex: 'identifierHash', key: 'identifierHash', ellipsis: true, width: 150, render: (v) => v ? v.substring(0, 16) + '...' : '-' },
    { title: 'Identity Type', dataIndex: 'identityType', key: 'identityType', width: 110 },
    { title: 'Auth Method', dataIndex: 'authMethod', key: 'authMethod', width: 110 },
    {
      title: 'Result', dataIndex: 'result', key: 'result', width: 120,
      render: (v: string) => (
        <Tag color={resultColors[v] || 'default'}>{v || '-'}</Tag>
      ),
    },
    {
      title: 'Failure Reason', dataIndex: 'failureReason', key: 'failureReason', width: 160, ellipsis: true,
      render: (v) => v || '-',
    },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 130, render: (v) => v || '-' },
    { title: 'Time', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>Login Logs</Title>
        <Button icon={<ReloadOutlined />} onClick={fetchLogs}>Refresh</Button>
      </div>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="User ID"
          style={{ width: 130 }}
          onSearch={(v) => setFilters({ ...filters, userId: v })}
          allowClear
        />
        <Select
          placeholder="Result"
          style={{ width: 150 }}
          allowClear
          onChange={(v) => setFilters({ ...filters, result: v })}
          options={[
            { value: 'success', label: 'Success' },
            { value: 'failed_credential', label: 'Failed (Credential)' },
            { value: 'failed_mfa', label: 'Failed (MFA)' },
            { value: 'failed_locked', label: 'Locked' },
            { value: 'failed_disabled', label: 'Disabled' },
          ]}
        />
        <Select
          placeholder="Identity Type"
          style={{ width: 150 }}
          allowClear
          onChange={(v) => setFilters({ ...filters, identityType: v })}
          options={[
            { value: 'EMAIL', label: 'Email' },
            { value: 'PHONE', label: 'Phone' },
            { value: 'USERNAME', label: 'Username' },
          ]}
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
    </Card>
  )
}

export default LoginLogList
