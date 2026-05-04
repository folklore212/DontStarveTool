import client from './client'
import type {
  R,
  IPageData,
  PageQuery,
  OAuthClientVO,
  OAuthClientCreateRequest,
  OAuthClientUpdateRequest,
} from '../types'

export function listClients(params: PageQuery) {
  return client.get<R<IPageData<OAuthClientVO>>>('/oauth/clients', { params })
}

export function getClient(id: number) {
  return client.get<R<OAuthClientVO>>(`/oauth/clients/${id}`)
}

export function createClient(data: OAuthClientCreateRequest) {
  return client.post<R<OAuthClientVO>>('/oauth/clients', data)
}

export function updateClient(id: number, data: OAuthClientUpdateRequest) {
  return client.put<R<OAuthClientVO>>(`/oauth/clients/${id}`, data)
}

export function deleteClient(id: number) {
  return client.delete<R<null>>(`/oauth/clients/${id}`)
}

export function regenerateSecret(id: number) {
  return client.post<R<{ clientId: string; clientSecret: string }>>(
    `/oauth/clients/${id}/regenerate-secret`,
  )
}
