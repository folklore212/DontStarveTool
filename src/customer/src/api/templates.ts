import client from './client'
import type { R, IPageData } from '../types/api'

export interface TemplateInfo {
  id: number
  authorId: number
  name: string
  description: string
  templateType: 'server_template' | 'world_gen'
  category: string
  gameMode: string
  maxPlayers: number
  tags: string
  coverImage: string
  configJson: string
  modList: string
  version: number
  downloadCount: number
  ratingAvg: number
  ratingCount: number
  status: string
  verified: number
  createdAt: string
  updatedAt: string
}

export interface WorldGenPresetInfo {
  id: number
  templateId: number
  name: string
  description: string
  previewImage: string
  worldSize: string
  branching: string
  loopMode: string
  seasonStart: string
  dayMode: string
  autumnLength: string
  winterLength: string
  springLength: string
  summerLength: string
  resourceVariety: string
  extraSettings: string
  sortOrder: number
}

export interface WorldGenMetadata {
  [key: string]: {
    label: string
    icon: string
    options: { value: string; label: string; icon: string }[]
  }
}

export interface TemplateFullDetail {
  template: TemplateInfo
  worldGenPresets: WorldGenPresetInfo[]
}

export interface WorkshopModInfo {
  workshopId: string
  title: string
  description: string
  previewUrl: string
  subscriptions: number
  favorited: number
  tags: string
}

// ---- Templates ----

export function browseTemplates(params: Record<string, unknown> = {}): Promise<R<IPageData<TemplateInfo>>> {
  const qs = new URLSearchParams(params as Record<string, string>).toString()
  return client.get(`/templates?${qs}`).then((r) => r.data)
}

export function getTemplateDetail(id: number): Promise<R<TemplateFullDetail>> {
  return client.get(`/templates/${id}`).then((r) => r.data)
}

export function createTemplate(data: Record<string, unknown>): Promise<R<TemplateInfo>> {
  return client.post('/templates', data).then((r) => r.data)
}

export function updateTemplate(id: number, data: Record<string, unknown>): Promise<R<TemplateInfo>> {
  return client.put(`/templates/${id}`, data).then((r) => r.data)
}

export function deleteTemplate(id: number): Promise<R<void>> {
  return client.delete(`/templates/${id}`).then((r) => r.data)
}

export function forkTemplate(id: number): Promise<R<TemplateInfo>> {
  return client.post(`/templates/${id}/fork`).then((r) => r.data)
}

export function getTemplateWorldGen(id: number): Promise<R<WorldGenPresetInfo[]>> {
  return client.get(`/templates/${id}/world-gen`).then((r) => r.data)
}

export function bindTemplateWorldGen(id: number, bindings: { presetId: number; shardType: string }[]): Promise<R<void>> {
  return client.put(`/templates/${id}/world-gen`, bindings).then((r) => r.data)
}

// ---- World Gen Presets ----

export function getWorldGenMetadata(): Promise<R<WorldGenMetadata>> {
  return client.get('/worldgen/metadata').then((r) => r.data)
}

export function browseWorldGenPresets(params: Record<string, unknown> = {}): Promise<R<IPageData<WorldGenPresetInfo>>> {
  const qs = new URLSearchParams(params as Record<string, string>).toString()
  return client.get(`/worldgen?${qs}`).then((r) => r.data)
}

export function getWorldGenPreset(id: number): Promise<R<WorldGenPresetInfo>> {
  return client.get(`/worldgen/${id}`).then((r) => r.data)
}

export function createWorldGenPreset(data: Record<string, unknown>): Promise<R<WorldGenPresetInfo>> {
  return client.post('/worldgen', data).then((r) => r.data)
}

export function updateWorldGenPreset(id: number, data: Record<string, unknown>): Promise<R<WorldGenPresetInfo>> {
  return client.put(`/worldgen/${id}`, data).then((r) => r.data)
}

export function deleteWorldGenPreset(id: number): Promise<R<void>> {
  return client.delete(`/worldgen/${id}`).then((r) => r.data)
}

// ---- Steam Workshop ----

export function getHotMods(): Promise<R<WorkshopModInfo[]>> {
  return client.get('/workshop/hot').then((r) => r.data)
}

export function searchWorkshopMods(keyword?: string): Promise<R<WorkshopModInfo[]>> {
  const qs = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  return client.get(`/workshop/search${qs}`).then((r) => r.data)
}
