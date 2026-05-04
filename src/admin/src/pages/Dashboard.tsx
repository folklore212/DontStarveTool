import { useEffect, useState } from 'react'
import { Card, Col, Row, Statistic, Typography, Spin, Table } from 'antd'
import {
  UserOutlined,
  TeamOutlined,
  AuditOutlined,
  KeyOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from '@ant-design/icons'
import * as usersApi from '../api/users'
import * as rolesApi from '../api/roles'
import * as auditApi from '../api/audit'
import type { UserVO } from '../types'

const { Title } = Typography

function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    disabledUsers: 0,
    totalRoles: 0,
    totalAuditLogs: 0,
    totalLoginAttempts: 0,
  })
  const [recentUsers, setRecentUsers] = useState<UserVO[]>([])

  useEffect(() => {
    async function fetchStats() {
      try {
        const [usersRes, activeRes, disabledRes, rolesRes, auditRes, loginRes, recentRes] =
          await Promise.allSettled([
            usersApi.listUsers({ page: 1, size: 1 }),
            usersApi.listUsers({ page: 1, size: 1, status: 1 }),
            usersApi.listUsers({ page: 1, size: 1, status: 0 }),
            rolesApi.listRoles(),
            auditApi.queryAuditLogs({ page: 1, size: 1 }),
            auditApi.queryLoginLogs({ page: 1, size: 1 }),
            usersApi.listUsers({ page: 1, size: 10, sortBy: 'created_at', sortOrder: 'DESC' }),
          ])

        setStats({
          totalUsers:
            usersRes.status === 'fulfilled' ? usersRes.value.data.data?.total ?? 0 : 0,
          activeUsers:
            activeRes.status === 'fulfilled' ? activeRes.value.data.data?.total ?? 0 : 0,
          disabledUsers:
            disabledRes.status === 'fulfilled' ? disabledRes.value.data.data?.total ?? 0 : 0,
          totalRoles:
            rolesRes.status === 'fulfilled' ? rolesRes.value.data.data?.length ?? 0 : 0,
          totalAuditLogs:
            auditRes.status === 'fulfilled' ? auditRes.value.data.data?.total ?? 0 : 0,
          totalLoginAttempts:
            loginRes.status === 'fulfilled' ? loginRes.value.data.data?.total ?? 0 : 0,
        })

        setRecentUsers(
          recentRes.status === 'fulfilled' ? recentRes.value.data.data?.records ?? [] : [],
        )
      } finally {
        setLoading(false)
      }
    }
    fetchStats()
  }, [])

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />

  return (
    <>
      <Title level={2}>Dashboard</Title>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Row justify="space-between" align="middle">
              <Col>
                <Statistic title="Total Users" value={stats.totalUsers} prefix={<UserOutlined />} />
              </Col>
              <Col>
                <span style={{ color: '#52c41a', marginRight: 16 }}>
                  <CheckCircleOutlined /> {stats.activeUsers} active
                </span>
                <span style={{ color: '#ff4d4f' }}>
                  <StopOutlined /> {stats.disabledUsers} disabled
                </span>
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic title="Roles" value={stats.totalRoles} prefix={<TeamOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <Card>
            <Statistic title="Audit Logs" value={stats.totalAuditLogs} prefix={<AuditOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <Card>
            <Statistic title="Login Attempts" value={stats.totalLoginAttempts} prefix={<KeyOutlined />} />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Users">
        <Table
          dataSource={recentUsers}
          rowKey="userId"
          pagination={false}
          columns={[
            { title: 'ID', dataIndex: 'userId', width: 80 },
            { title: 'Username', dataIndex: 'username' },
            { title: 'Email', dataIndex: 'email', render: (v) => v || '-' },
            { title: 'Status', dataIndex: 'status', width: 90,
              render: (s: number) => {
                const labels: Record<number, string> = { 0: 'Normal', 1: 'Disabled', 2: 'Pending', 3: 'Locked' }
                const colors: Record<number, string> = { 0: '#52c41a', 1: '#ff4d4f', 2: '#faad14', 3: '#ff4d4f' }
                return <span style={{ color: colors[s] || '#999' }}>{labels[s] || String(s)}</span>
              } },
            { title: 'Created', dataIndex: 'createdAt', width: 170 },
          ]}
        />
      </Card>
    </>
  )
}

export default Dashboard
