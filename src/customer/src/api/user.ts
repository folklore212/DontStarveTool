import client from './client'
import type { R } from '../types/api'

export interface UserVO {
  userId: number
  username: string
  email: string | null
  phone: string | null
  nickname: string | null
  avatar: string | null
  status: number
  lockedUntil: number | null
  lastLoginAt: string | null
  lastLoginIp: string | null
  passwordChangedAt: string | null
  createdAt: string | null
}

export interface UpdateProfileRequest {
  realName?: string
  locale?: string
  timezone?: string
  metadata?: string
}

export interface UpdateNicknameRequest {
  nickname: string
}

/** GET /users/me */
export function getCurrentUser(): Promise<R<UserVO>> {
  return client.get('/users/me').then((res) => res.data)
}

/** PUT /users/me/profile */
export function updateProfile(data: UpdateProfileRequest): Promise<R<UserVO>> {
  return client.put('/users/me/profile', data).then((res) => res.data)
}

/** PUT /users/me/nickname */
export function updateNickname(data: UpdateNicknameRequest): Promise<R<UserVO>> {
  return client.put('/users/me/nickname', data).then((res) => res.data)
}

/** PUT /users/me/avatar */
export function updateAvatar(avatar: string): Promise<R<UserVO>> {
  return client.put('/users/me/avatar', { avatar }).then((res) => res.data)
}
