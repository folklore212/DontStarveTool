// ---- Generic API response wrapper ----
// Mirrors backend com.iccuu.general_web_backend.common.result.R<T>
export interface R<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface FieldError {
  field: string
  message: string
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
