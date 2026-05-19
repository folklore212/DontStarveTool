import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Tag,
  Popconfirm,
  message,
  Typography,
  DatePicker,
} from 'antd'
import {
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  SafetyOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UserVO, UserRoleVO, UserAuthVO, ScopeVO } from '../../types'
import * as usersApi from '../../api/users'
import * as rolesApi from '../../api/roles'

const { Title } = Typography
const { RangePicker } = DatePicker

function UserList() {
  const [users, setUsers] = useState<UserVO[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [rolesOpen, setRolesOpen] = useState(false)
  const [authsOpen, setAuthsOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserVO | null>(null)
  const [userRoles, setUserRoles] = useState<UserRoleVO[]>([])
  const [userAuths, setUserAuths] = useState<UserAuthVO[]>([])
  const [allRoles, setAllRoles] = useState<{ id: number; roleName: string }[]>([])
  const [scopes, setScopes] = useState<ScopeVO[]>([])
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [rolesForm] = Form.useForm()
  const [authsForm] = Form.useForm()

  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<number | undefined>()
  const [dateRange, setDateRange] = useState<[string, string] | null>(null)

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { page, size }
      if (keyword) params.keyword = keyword
      if (statusFilter !== undefined) params.status = statusFilter
      if (dateRange) {
        params.startDate = dateRange[0]
        params.endDate = dateRange[1]
      }
      const res = await usersApi.listUsers(params)
      const d = res.data.data
      setUsers(d?.records ?? [])
      setTotal(d?.total ?? 0)
    } catch {
      message.error('Failed to fetch users')
    } finally {
      setLoading(false)
    }
  }, [page, size, keyword, statusFilter, dateRange])

  useEffect(() => { fetchUsers() }, [fetchUsers])

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      await usersApi.createUser({
        username: values.username as string,
        password: values.password as string,
        email: values.email as string | undefined,
        phone: values.phone as string | undefined,
        nickname: values.nickname as string | undefined,
      })
      message.success('User created')
      setCreateOpen(false)
      form.resetFields()
      fetchUsers()
    } catch {
      message.error('Failed to create user')
    }
  }

  const handleEdit = async (values: Record<string, unknown>) => {
    if (!selectedUser) return
    try {
      await usersApi.updateUser(selectedUser.userId, values)
      message.success('User updated')
      setEditOpen(false)
      editForm.resetFields()
      fetchUsers()
    } catch {
      message.error('Failed to update user')
    }
  }

  const handleDelete = async (userId: number) => {
    try {
      await usersApi.deleteUser(userId)
      message.success('User deleted')
      fetchUsers()
    } catch {
      message.error('Failed to delete user')
    }
  }

  const statusLabels: Record<number, string> = { 0: 'Normal', 1: 'Disabled', 2: 'Pending', 3: 'Locked' }

  const handleUpdateStatus = async (userId: number, newStatus: number) => {
    try {
      await usersApi.updateUserStatus(userId, { status: newStatus })
      message.success(`User status updated to ${statusLabels[newStatus]}`)
      fetchUsers()
    } catch {
      message.error('Failed to update status')
    }
  }

  const openRoles = async (user: UserVO) => {
    setSelectedUser(user)
    setRolesOpen(true)
    rolesForm.resetFields()
    try {
      const [urRes, arRes, scRes] = await Promise.all([
        usersApi.getUserRoles(user.userId),
        rolesApi.listRoles(),
        rolesApi.listScopes(),
      ])
      setUserRoles(urRes.data.data ?? [])
      setAllRoles((arRes.data.data ?? []).map((r) => ({ id: r.id, roleName: r.roleName })))
      setScopes(scRes.data.data ?? [])
    } catch {
      message.error('Failed to fetch roles')
    }
  }

  const handleAssignRole = async (values: { roleId: number; scopeType?: string; scopeValue?: string }) => {
    if (!selectedUser) return
    try {
      await usersApi.assignUserRoles(selectedUser.userId, {
        roleIds: [values.roleId],
        scopeType: values.scopeType || undefined,
        scopeValue: values.scopeValue || undefined,
      })
      message.success('Role assigned')
      rolesForm.resetFields()
      openRoles(selectedUser)
    } catch {
      message.error('Failed to assign role')
    }
  }

  const handleRemoveRole = async (roleId: number, scopeType: string, scopeValue: string) => {
    if (!selectedUser) return
    try {
      await usersApi.removeUserRole(
        selectedUser.userId,
        roleId,
        scopeType || 'global',
        scopeValue || 'all',
      )
      message.success('Role removed')
      openRoles(selectedUser)
    } catch {
      message.error('Failed to remove role')
    }
  }

  const openAuths = async (user: UserVO) => {
    setSelectedUser(user)
    setAuthsOpen(true)
    authsForm.resetFields()
    try {
      const res = await usersApi.getUserAuths(user.userId)
      setUserAuths(res.data.data ?? [])
    } catch {
      message.error('Failed to fetch auths')
    }
  }

  const handleBindAuth = async (values: Record<string, unknown>) => {
    if (!selectedUser) return
    try {
      await usersApi.bindIdentity(selectedUser.userId, {
        identityType: values.identityType as string,
        identifier: values.identifier as string,
        credential: values.credential as string | undefined,
        isPrimary: values.isPrimary as number | undefined,
      })
      message.success('Identity bound')
      authsForm.resetFields()
      openAuths(selectedUser)
    } catch {
      message.error('Failed to bind identity')
    }
  }

  const handleUnbindAuth = async (authId: number) => {
    if (!selectedUser) return
    try {
      await usersApi.unbindIdentity(selectedUser.userId, authId)
      message.success('Identity unbound')
      openAuths(selectedUser)
    } catch {
      message.error('Failed to unbind identity')
    }
  }

  const columns: ColumnsType<UserVO> = [
    { title: 'ID', dataIndex: 'userId', key: 'userId', width: 80 },
    { title: 'Username', dataIndex: 'username', key: 'username', ellipsis: true },
    { title: 'Email', dataIndex: 'email', key: 'email', ellipsis: true, render: (v) => v || '-' },
    { title: 'Nickname', dataIndex: 'nickname', key: 'nickname', ellipsis: true, render: (v) => v || '-' },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 90,
      render: (s: number) => {
        const colors: Record<number, string> = { 0: 'green', 1: 'red', 2: 'orange', 3: 'red' }
        return <Tag color={colors[s] || 'default'}>{statusLabels[s] || s}</Tag>
      },
    },
    {
      title: 'Last Login', dataIndex: 'lastLoginAt', key: 'lastLoginAt', width: 170,
      render: (v) => v || '-',
    },
    {
      title: 'Actions', key: 'actions', width: 330,
      render: (_: unknown, record: UserVO) => (
        <Space>
          <Select
            size="small"
            value={record.status}
            disabled={record.status === 3}
            style={{ width: 100 }}
            onChange={(v) => handleUpdateStatus(record.userId, v)}
            options={[
              { value: 0, label: 'Normal' },
              { value: 1, label: 'Disabled' },
              { value: 2, label: 'Pending' },
              { value: 3, label: 'Locked' },
            ]}
          />
          <Button
            size="small"
            icon={<TeamOutlined />}
            onClick={() => openRoles(record)}
          >
            Roles
          </Button>
          <Button
            size="small"
            icon={<SafetyOutlined />}
            onClick={() => openAuths(record)}
          >
            Auths
          </Button>
          <Button
            size="small"
            onClick={() => {
              setSelectedUser(record)
              editForm.setFieldsValue(record)
              setEditOpen(true)
            }}
          >
            Edit
          </Button>
          <Popconfirm
            title="Delete this user?"
            onConfirm={() => handleDelete(record.userId)}
          >
            <Button size="small" danger>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>Users</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchUsers}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            New User
          </Button>
        </Space>
      </div>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="Search username / email"
          style={{ width: 260 }}
          allowClear
          onSearch={(v) => { setKeyword(v); setPage(1) }}
        />
        <Select
          placeholder="Status"
          style={{ width: 130 }}
          allowClear
          onChange={(v) => { setStatusFilter(v); setPage(1) }}
          options={[
            { value: 0, label: 'Normal' },
            { value: 1, label: 'Disabled' },
            { value: 2, label: 'Pending' },
            { value: 3, label: 'Locked' },
          ]}
        />
        <RangePicker
          placeholder={['Start Date', 'End Date']}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0].format('YYYY-MM-DD'), dates[1].format('YYYY-MM-DD')])
            } else {
              setDateRange(null)
            }
            setPage(1)
          }}
        />
        <Button
          onClick={() => { setKeyword(''); setStatusFilter(undefined); setDateRange(null); setPage(1) }}
        >
          Clear Filters
        </Button>
      </Space>

      <Table
        dataSource={users}
        columns={columns}
        rowKey="userId"
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

      {/* Create Modal */}
      <Modal
        title="Create User"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="username" label="Username" rules={[{ required: true, min: 3, max: 50 }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true, min: 6 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="email" label="Email">
            <Input type="email" />
          </Form.Item>
          <Form.Item name="phone" label="Phone">
            <Input />
          </Form.Item>
          <Form.Item name="nickname" label="Nickname">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title="Edit User"
        open={editOpen}
        onCancel={() => { setEditOpen(false); editForm.resetFields() }}
        onOk={() => editForm.submit()}
      >
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item name="email" label="Email">
            <Input type="email" />
          </Form.Item>
          <Form.Item name="phone" label="Phone">
            <Input />
          </Form.Item>
          <Form.Item name="nickname" label="Nickname">
            <Input />
          </Form.Item>
          <Form.Item name="avatar" label="Avatar URL">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      {/* Roles Modal */}
      <Modal
        title={`Roles — ${selectedUser?.username ?? ''}`}
        open={rolesOpen}
        onCancel={() => setRolesOpen(false)}
        footer={null}
        width={680}
      >
        <Table
          dataSource={userRoles}
          rowKey={(r) => `${r.roleId}-${r.scopeType}-${r.scopeValue}`}
          columns={[
            { title: 'Role', dataIndex: 'roleName', key: 'roleName' },
            { title: 'Scope Type', dataIndex: 'scopeType', key: 'scopeType', render: (v) => v || 'global' },
            { title: 'Scope Value', dataIndex: 'scopeValue', key: 'scopeValue', render: (v) => v || 'all' },
            {
              title: 'Action', key: 'action', width: 80,
              render: (_: unknown, r: UserRoleVO) => (
                <Popconfirm
                  title="Remove this role?"
                  onConfirm={() => handleRemoveRole(r.roleId, r.scopeType, r.scopeValue)}
                >
                  <Button size="small" danger>Remove</Button>
                </Popconfirm>
              ),
            },
          ]}
          pagination={false}
          style={{ marginBottom: 16 }}
        />
        <Form form={rolesForm} layout="inline" onFinish={handleAssignRole} style={{ marginTop: 16, gap: 8 }}>
          <Form.Item name="roleId" rules={[{ required: true, message: 'Select a role' }]}>
            <Select style={{ width: 180 }} placeholder="Select role">
              {allRoles.map((r) => (
                <Select.Option key={r.id} value={r.id}>{r.roleName}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="scopeType">
            <Select style={{ width: 150 }} placeholder="Scope type" allowClear>
              {scopes.map((s) => (
                <Select.Option key={s.scopeKey} value={s.scopeKey}>{s.scopeKey}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="scopeValue">
            <Input style={{ width: 140 }} placeholder="Scope value" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">Assign</Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Auths Modal */}
      <Modal
        title={`Identity Auths — ${selectedUser?.username ?? ''}`}
        open={authsOpen}
        onCancel={() => setAuthsOpen(false)}
        footer={null}
        width={680}
      >
        <Table
          dataSource={userAuths}
          rowKey="id"
          columns={[
            { title: 'ID', dataIndex: 'id', width: 60 },
            { title: 'Type', dataIndex: 'identityType', key: 'identityType', width: 100,
              render: (v: string) => <Tag>{v}</Tag> },
            { title: 'Identifier', dataIndex: 'identifier', key: 'identifier' },
            { title: 'Verified', dataIndex: 'verified', key: 'verified', width: 90,
              render: (v: number) => v === 1 ? <Tag color="green">Yes</Tag> : <Tag color="orange">No</Tag> },
            { title: 'Primary', dataIndex: 'isPrimary', key: 'isPrimary', width: 80,
              render: (v: number) => v === 1 ? <Tag color="blue">Yes</Tag> : '-' },
            { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
            {
              title: 'Action', key: 'action', width: 80,
              render: (_: unknown, r: UserAuthVO) => (
                <Popconfirm
                  title="Unbind this identity?"
                  onConfirm={() => handleUnbindAuth(r.id)}
                >
                  <Button size="small" danger>Unbind</Button>
                </Popconfirm>
              ),
            },
          ]}
          pagination={false}
          style={{ marginBottom: 16 }}
        />
        <Form form={authsForm} layout="inline" onFinish={handleBindAuth} style={{ marginTop: 16, gap: 8 }}>
          <Form.Item name="identityType" rules={[{ required: true }]}>
            <Select style={{ width: 130 }} placeholder="Type">
              <Select.Option value="EMAIL">Email</Select.Option>
              <Select.Option value="PHONE">Phone</Select.Option>
              <Select.Option value="USERNAME">Username</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="identifier" rules={[{ required: true }]}>
            <Input style={{ width: 170 }} placeholder="Identifier" />
          </Form.Item>
          <Form.Item name="credential">
            <Input style={{ width: 140 }} placeholder="Credential (optional)" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">Bind</Button>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}

export default UserList
