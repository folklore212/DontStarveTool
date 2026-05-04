import client from './client'
import type {
  R,
  IPageData,
  UserVO,
  UserQueryRequest,
  UserCreateRequest,
  UserUpdateRequest,
  UserStatusRequest,
  UserRoleVO,
  UserAuthVO,
  BindAuthRequest,
} from '../types'

export function listUsers(params: UserQueryRequest | Record<string, unknown>) {
  return client.get<R<IPageData<UserVO>>>('/users', { params })
}

export function getUserById(userId: number) {
  return client.get<R<UserVO>>(`/users/${userId}`)
}

export function createUser(data: UserCreateRequest) {
  return client.post<R<UserVO>>('/users/', data)
}

export function updateUser(userId: number, data: UserUpdateRequest) {
  return client.put<R<UserVO>>(`/users/${userId}`, data)
}

export function deleteUser(userId: number) {
  return client.delete<R<null>>(`/users/${userId}`)
}

export function updateUserStatus(userId: number, data: UserStatusRequest) {
  return client.patch<R<null>>(`/users/${userId}/status`, data)
}

export function getUserRoles(userId: number) {
  return client.get<R<UserRoleVO[]>>(`/users/${userId}/roles`)
}

export function assignUserRoles(userId: number, data: {
  roleIds: number[]
  scopeType?: string
  scopeValue?: string
  expiresAt?: string
}) {
  return client.post<R<null>>(`/users/${userId}/roles`, data)
}

export function removeUserRole(
  userId: number,
  roleId: number,
  scopeType: string,
  scopeValue: string,
) {
  return client.delete<R<null>>(
    `/users/${userId}/roles/${roleId}/${scopeType}/${scopeValue}`,
  )
}

export function getUserAuths(userId: number) {
  return client.get<R<UserAuthVO[]>>(`/users/${userId}/auths`)
}

export function bindIdentity(userId: number, data: BindAuthRequest) {
  return client.post<R<null>>(`/users/${userId}/auths`, data)
}

export function unbindIdentity(userId: number, authId: number) {
  return client.delete<R<null>>(`/users/${userId}/auths/${authId}`)
}
