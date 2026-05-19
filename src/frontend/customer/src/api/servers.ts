import client from './client'
import type { R, IPageData } from '../types/api'

export interface ServerInfo {
  id: number
  userId: number
  name: string
  host: string
  port: number
  username: string
  authType: string
  status: string
  osInfo: string
  tags: string
  createdAt: string
  updatedAt: string
}

export interface DstClusterInfo {
  id: number
  serverId: number
  name: string
  displayName: string
  gameMode: string
  maxPlayers: number
  status: string
  playerCount: number
  dayCount: number
  season: number
  createdAt: string
}

export function listServers(page = 1, size = 20): Promise<R<IPageData<ServerInfo>>> {
  return client.get(`/servers?page=${page}&size=${size}`).then((r) => r.data)
}

export function createServer(data: Partial<ServerInfo>): Promise<R<ServerInfo>> {
  return client.post('/servers', data).then((r) => r.data)
}

export function updateServer(id: number, data: Partial<ServerInfo>): Promise<R<ServerInfo>> {
  return client.put(`/servers/${id}`, data).then((r) => r.data)
}

export function deleteServer(id: number): Promise<R<void>> {
  return client.delete(`/servers/${id}`).then((r) => r.data)
}

export function testConnection(id: number): Promise<R<{ success: boolean; message: string; elapsed: number }>> {
  return client.post(`/servers/${id}/test`).then((r) => r.data)
}

export function listClusters(serverId: number): Promise<R<DstClusterInfo[]>> {
  return client.get(`/servers/${serverId}/clusters`).then((r) => r.data)
}

export function createCluster(serverId: number, data: Record<string, unknown>): Promise<R<DstClusterInfo>> {
  return client.post(`/servers/${serverId}/clusters`, data).then((r) => r.data)
}

export function deleteCluster(serverId: number, clusterId: number): Promise<R<void>> {
  return client.delete(`/servers/${serverId}/clusters/${clusterId}`).then((r) => r.data)
}

export function installCluster(serverId: number, clusterId: number): Promise<R<Record<string, unknown>>> {
  return client.post(`/servers/${serverId}/clusters/${clusterId}/install`).then((r) => r.data)
}

export function startCluster(serverId: number, clusterId: number): Promise<R<{ success: boolean; output: string }>> {
  return client.post(`/servers/${serverId}/clusters/${clusterId}/start`).then((r) => r.data)
}

export function stopCluster(serverId: number, clusterId: number): Promise<R<{ success: boolean }>> {
  return client.post(`/servers/${serverId}/clusters/${clusterId}/stop`).then((r) => r.data)
}

export function clusterStatus(serverId: number, clusterId: number): Promise<R<{ status: string; output: string }>> {
  return client.get(`/servers/${serverId}/clusters/${clusterId}/status`).then((r) => r.data)
}

export function sendCommand(serverId: number, clusterId: number, command: string): Promise<R<{ success: boolean }>> {
  return client.post(`/servers/${serverId}/clusters/${clusterId}/console`, { command }).then((r) => r.data)
}

export function createBackup(serverId: number, clusterId: number): Promise<R<{ success: boolean; backupName: string; size: string }>> {
  return client.post(`/servers/${serverId}/clusters/${clusterId}/backup`).then((r) => r.data)
}
