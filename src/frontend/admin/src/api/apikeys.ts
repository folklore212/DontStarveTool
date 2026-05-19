import client from './client'
import type {
  R,
  IPageData,
  PageQuery,
  ApiKeyVO,
  ApiKeyCreateRequest,
  ApiKeyCreateResponse,
} from '../types'

export function listApiKeys(params: PageQuery) {
  return client.get<R<IPageData<ApiKeyVO>>>('/api-keys', { params })
}

export function createApiKey(data: ApiKeyCreateRequest) {
  return client.post<R<ApiKeyCreateResponse>>('/api-keys', data)
}

export function revokeApiKey(keyId: number) {
  return client.delete<R<null>>(`/api-keys/${keyId}`)
}

export function rotateApiKey(keyId: number) {
  return client.patch<R<ApiKeyCreateResponse>>(`/api-keys/${keyId}/rotate`)
}
