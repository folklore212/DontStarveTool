// ---- Generic API response ----
export interface R<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// MyBatis Plus IPage serialization shape
export interface IPageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageQuery {
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: 'ASC' | 'DESC'
}

// ---- Auth ----
export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  expiresIn?: number
}

// ---- User ----
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
  createdAt: string
}

export interface UserQueryRequest extends PageQuery {
  status?: number
  keyword?: string
  startDate?: string
  endDate?: string
}

export interface UserCreateRequest {
  username: string
  email?: string
  phone?: string
  nickname?: string
  password: string
}

export interface UserUpdateRequest {
  email?: string
  phone?: string
  nickname?: string
  avatar?: string
}

export interface UserStatusRequest {
  status: number
  lockedUntil?: number
}

export interface UserRoleVO {
  userId: number
  roleId: number
  roleName: string
  scopeType: string
  scopeValue: string
  expiresAt: string | null
}

export interface UserAuthVO {
  id: number
  identityType: string
  identifier: string
  verified: number
  isPrimary: number
  createdAt: string
}

export interface BindAuthRequest {
  identityType: string
  identifier: string
  credential?: string
  isPrimary?: number
}

export interface UserProfileVO {
  userId: number
  realName: string | null
  locale: string | null
  timezone: string | null
  metadata: string | null
}

export interface UserProfileUpdateRequest {
  realName?: string
  locale?: string
  timezone?: string
  metadata?: string
}

export interface MfaStatusVO {
  mfaType: string
  enabled: boolean
}

export interface MfaSetupInitResponse {
  qrCodeUrl: string
  secret: string
  backupCodes: string[]
}

// ---- Role ----
export interface RoleVO {
  id: number
  roleName: string
  description: string | null
  parentRoleId: number | null
  isSystem: number
  children?: RoleVO[]
  permissions?: PermissionVO[]
  createdAt: string | null
}

export interface RoleTreeVO {
  id: number
  roleName: string
  description: string | null
  parentRoleId: number | null
  children?: RoleTreeVO[]
}

export interface RoleCreateRequest {
  roleName: string
  description?: string
  parentRoleId?: number
  isSystem?: number
}

export interface RoleUpdateRequest {
  roleName?: string
  description?: string
  parentRoleId?: number
}

export interface PermissionVO {
  id: number
  code: string
  name: string
  resourceType: string
  action: string
  description: string | null
}

export interface AssignRoleRequest {
  roleIds: number[]
  scopeType?: string
  scopeValue?: string
  expiresAt?: string
}

export interface AssignPermissionRequest {
  permissionIds: number[]
}

export interface ScopeVO {
  id: number
  scopeKey: string
  description: string | null
}

// ---- OAuth Client ----
export interface OAuthClientVO {
  id: number
  clientId: string
  clientName: string
  clientType: string
  grantTypes: string
  redirectUris: string
  allowedScopes: string
  isTrusted: number
  status: number
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface OAuthClientCreateRequest {
  clientId: string
  clientName: string
  clientType?: string
  grantTypes?: string
  redirectUris?: string
  allowedScopes?: string
  isTrusted?: number
}

export interface OAuthClientUpdateRequest {
  clientName?: string
  clientType?: string
  grantTypes?: string
  redirectUris?: string
  allowedScopes?: string
  isTrusted?: number
}

// ---- API Key ----
export interface ApiKeyVO {
  id: number
  keyName: string
  keyPrefix: string
  allowedScopes: string | null
  expiresAt: string | null
  lastUsedAt: string | null
  status: number
  createdAt: string
}

export interface ApiKeyCreateRequest {
  keyName: string
  allowedScopes?: string
  expiresAt?: string
}

export interface ApiKeyCreateResponse {
  keyPrefix: string
  rawKey: string
  expiresAt: string | null
}

// ---- Audit Log ----
export interface AuditLogVO {
  id: number
  userId: number | null
  clientId: string | null
  action: string
  resourceType: string
  resourceId: string | null
  detail: string | null
  ipAddress: string | null
  sessionId: string | null
  requestId: string | null
  clientIpChain: string | null
  createdAt: string
}

export interface AuditLogQueryRequest extends PageQuery {
  userId?: number
  clientId?: string
  action?: string
  resourceType?: string
  startDate?: string
  endDate?: string
}

// ---- Login Log ----
export interface LoginLogVO {
  id: number
  userId: number | null
  identifierHash: string
  identityType: string
  authMethod: string
  ipAddress: string | null
  result: string
  failureReason: string | null
  createdAt: string
}

export interface LoginLogQueryRequest extends PageQuery {
  userId?: number
  result?: string
  identityType?: string
  startDate?: string
  endDate?: string
}
