import { useEffect, useState, useCallback } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Tag,
  Popconfirm,
  message,
  Typography,
  Transfer,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { RoleVO, PermissionVO } from '../../types'
import * as rolesApi from '../../api/roles'

const { Title } = Typography

function RoleList() {
  const [roles, setRoles] = useState<RoleVO[]>([])
  const [loading, setLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [permOpen, setPermOpen] = useState(false)
  const [treeOpen, setTreeOpen] = useState(false)
  const [treeData, setTreeData] = useState<RoleVO[]>([])
  const [selectedRole, setSelectedRole] = useState<RoleVO | null>(null)
  const [, setRolePermissions] = useState<PermissionVO[]>([])
  const [allPermissions, setAllPermissions] = useState<PermissionVO[]>([])
  const [targetKeys, setTargetKeys] = useState<string[]>([])
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()

  const fetchRoles = useCallback(async () => {
    setLoading(true)
    try {
      const res = await rolesApi.listRoles()
      setRoles(res.data.data ?? [])
    } catch {
      message.error('Failed to fetch roles')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchRoles() }, [fetchRoles])

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      await rolesApi.createRole(values as { roleName: string; description?: string; parentRoleId?: number })
      message.success('Role created')
      setCreateOpen(false)
      form.resetFields()
      fetchRoles()
    } catch {
      message.error('Failed to create role')
    }
  }

  const handleEdit = async (values: Record<string, unknown>) => {
    if (!selectedRole) return
    try {
      await rolesApi.updateRole(selectedRole.id, values)
      message.success('Role updated')
      setEditOpen(false)
      editForm.resetFields()
      fetchRoles()
    } catch {
      message.error('Failed to update role')
    }
  }

  const handleDelete = async (roleId: number) => {
    try {
      await rolesApi.deleteRole(roleId)
      message.success('Role deleted')
      fetchRoles()
    } catch {
      message.error('Failed to delete role')
    }
  }

  const openPermissions = async (role: RoleVO) => {
    setSelectedRole(role)
    setPermOpen(true)
    try {
      const [rpRes, apRes] = await Promise.all([
        rolesApi.getRolePermissions(role.id),
        rolesApi.listAllPermissions(),
      ])
      const perms = rpRes.data.data ?? []
      const all = apRes.data.data ?? []
      setRolePermissions(perms)
      setAllPermissions(all)
      setTargetKeys(perms.map((p) => String(p.id)))
    } catch {
      message.error('Failed to fetch permissions')
    }
  }

  const handleSavePermissions = async () => {
    if (!selectedRole) return
    try {
      await rolesApi.assignPermissions(selectedRole.id, {
        permissionIds: targetKeys.map(Number),
      })
      message.success('Permissions updated')
      setPermOpen(false)
    } catch {
      message.error('Failed to update permissions')
    }
  }

  const fetchTree = async () => {
    try {
      const res = await rolesApi.getRoleTree()
      setTreeData((res.data.data ?? []) as unknown as RoleVO[])
      setTreeOpen(true)
    } catch {
      message.error('Failed to fetch role tree')
    }
  }

  const columns: ColumnsType<RoleVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Name', dataIndex: 'roleName', key: 'roleName' },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true, render: (v) => v || '-' },
    {
      title: 'System', dataIndex: 'isSystem', key: 'isSystem', width: 80,
      render: (v: number) => v ? <Tag color="blue">System</Tag> : <Tag>Custom</Tag>,
    },
    { title: 'Parent ID', dataIndex: 'parentRoleId', key: 'parentRoleId', width: 90, render: (v) => v ?? '-' },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 170, render: (v) => v || '-' },
    {
      title: 'Actions', key: 'actions', width: 260,
      render: (_: unknown, record: RoleVO) => (
        <Space>
          <Button size="small" disabled={record.isSystem === 1} onClick={() => openPermissions(record)}>Permissions</Button>
          <Button size="small" disabled={record.isSystem === 1} onClick={() => {
            setSelectedRole(record)
            editForm.setFieldsValue(record)
            setEditOpen(true)
          }}>Edit</Button>
          <Popconfirm
            title="Delete this role?"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button size="small" danger disabled={record.isSystem === 1}>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const treeColumns: ColumnsType<RoleVO> = [
    { title: 'Name', dataIndex: 'roleName', key: 'roleName' },
    { title: 'Description', dataIndex: 'description', key: 'description', render: (v) => v || '-' },
    { title: 'Parent ID', dataIndex: 'parentRoleId', key: 'parentRoleId', width: 90, render: (v) => v ?? '-' },
    {
      title: 'System', dataIndex: 'isSystem', key: 'isSystem', width: 80,
      render: (v: number) => v ? <Tag color="blue">System</Tag> : <Tag>Custom</Tag>,
    },
  ]

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>Roles</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchRoles}>Refresh</Button>
          <Button onClick={fetchTree}>Tree View</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            New Role
          </Button>
        </Space>
      </div>

      <Table
        dataSource={roles}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      {/* Create Modal */}
      <Modal
        title="Create Role"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="roleName" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="parentRoleId" label="Parent Role ID">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title="Edit Role"
        open={editOpen}
        onCancel={() => { setEditOpen(false); editForm.resetFields() }}
        onOk={() => editForm.submit()}
      >
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item name="roleName" label="Name">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="parentRoleId" label="Parent Role ID">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Permissions Modal */}
      <Modal
        title={`Permissions — ${selectedRole?.roleName ?? ''}`}
        open={permOpen}
        onCancel={() => setPermOpen(false)}
        onOk={handleSavePermissions}
        width={700}
      >
        <Transfer
          dataSource={allPermissions.map((p) => ({
            key: String(p.id),
            title: `${p.name} (${p.code})`,
            description: p.resourceType,
          }))}
          targetKeys={targetKeys}
          onChange={(keys) => setTargetKeys(keys as string[])}
          render={(item) => item.title}
          listStyle={{ width: 300, height: 400 }}
        />
      </Modal>

      {/* Tree View Modal */}
      <Modal
        title="Role Hierarchy"
        open={treeOpen}
        onCancel={() => setTreeOpen(false)}
        footer={null}
        width={700}
      >
        <Table
          dataSource={treeData}
          columns={treeColumns}
          rowKey="id"
          pagination={false}
          expandable={{ defaultExpandAllRows: true }}
        />
      </Modal>
    </Card>
  )
}

export default RoleList
