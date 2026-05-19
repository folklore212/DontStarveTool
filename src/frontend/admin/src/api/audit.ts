import client from './client'
import type {
  R,
  IPageData,
  AuditLogVO,
  AuditLogQueryRequest,
  LoginLogVO,
  LoginLogQueryRequest,
} from '../types'

export function queryAuditLogs(params: AuditLogQueryRequest | Record<string, unknown>) {
  return client.get<R<IPageData<AuditLogVO>>>('/audit-logs', { params })
}

export function getAuditLog(id: number) {
  return client.get<R<AuditLogVO>>(`/audit-logs/${id}`)
}

export function queryLoginLogs(params: LoginLogQueryRequest | Record<string, unknown>) {
  return client.get<R<IPageData<LoginLogVO>>>('/login-logs', { params })
}
