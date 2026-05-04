import client from './client'
import type { R, IPageData } from '../types/api'

export interface MarketConfigInfo {
  id: number; authorId: number; title: string; description: string
  tags: string; category: string; gameMode: string
  downloadCount: number; ratingAvg: number; ratingCount: number
  version: number; status: string; verified: number
  createdAt: string
}

export function browseMarketplace(params: Record<string, unknown> = {}): Promise<R<IPageData<MarketConfigInfo>>> {
  const qs = new URLSearchParams(params as Record<string, string>).toString()
  return client.get(`/marketplace?${qs}`).then((r) => r.data)
}

export function getMarketConfig(id: number): Promise<R<MarketConfigInfo>> {
  return client.get(`/marketplace/${id}`).then((r) => r.data)
}

export function publishConfig(data: Record<string, unknown>): Promise<R<MarketConfigInfo>> {
  return client.post('/marketplace', data).then((r) => r.data)
}

export function forkConfig(id: number): Promise<R<MarketConfigInfo>> {
  return client.post(`/marketplace/${id}/fork`).then((r) => r.data)
}

export function reviewConfig(id: number, rating: number): Promise<R<void>> {
  return client.post(`/marketplace/${id}/review`, { rating }).then((r) => r.data)
}

export function deployConfig(id: number, serverId: number): Promise<R<Record<string, unknown>>> {
  return client.post(`/marketplace/${id}/deploy`, { serverId }).then((r) => r.data)
}
