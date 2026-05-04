import client from './client'
import type {
  R,
  RoleVO,
  RoleTreeVO,
  RoleCreateRequest,
  RoleUpdateRequest,
  PermissionVO,
  ScopeVO,
  AssignPermissionRequest,
} from '../types'

export function listRoles() {
  return client.get<R<RoleVO[]>>('/roles')
}

export function getRoleTree() {
  return client.get<R<RoleTreeVO[]>>('/roles/tree')
}

export function getRole(roleId: number) {
  return client.get<R<RoleVO>>(`/roles/${roleId}`)
}

export function createRole(data: RoleCreateRequest) {
  return client.post<R<RoleVO>>('/roles', data)
}

export function updateRole(roleId: number, data: RoleUpdateRequest) {
  return client.put<R<RoleVO>>(`/roles/${roleId}`, data)
}

export function deleteRole(roleId: number) {
  return client.delete<R<null>>(`/roles/${roleId}`)
}

export function getRolePermissions(roleId: number) {
  return client.get<R<PermissionVO[]>>(`/roles/${roleId}/permissions`)
}

export function assignPermissions(roleId: number, data: AssignPermissionRequest) {
  return client.post<R<null>>(`/roles/${roleId}/permissions`, data)
}

export function removePermission(roleId: number, permId: number) {
  return client.delete<R<null>>(`/roles/${roleId}/permissions/${permId}`)
}

export function listAllPermissions() {
  return client.get<R<PermissionVO[]>>('/permissions')
}

export function listScopes() {
  return client.get<R<ScopeVO[]>>('/scopes')
}
