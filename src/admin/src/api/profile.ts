import client from './client'
import type { R, UserVO, UserProfileVO, UserProfileUpdateRequest } from '../types'

export function getCurrentUser() {
  return client.get<R<UserVO>>('/users/me')
}

export function getCurrentProfile() {
  return client.get<R<UserProfileVO>>('/users/me/profile')
}

export function updateProfile(data: UserProfileUpdateRequest) {
  return client.put<R<UserVO>>('/users/me/profile', data)
}

export function updateAvatar(avatar: string) {
  return client.put<R<UserVO>>('/users/me/avatar', { avatar })
}
