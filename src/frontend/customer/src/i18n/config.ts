export interface LocaleConfig {
  code: string // ISO 639-1 + region: 'zh-CN', 'en'
  label: string // Native name: '简体中文', 'English'
  antdLocale: () => Promise<any> // Dynamic import of antd locale
}

export const SUPPORTED_LOCALES: LocaleConfig[] = [
  {
    code: 'zh-CN',
    label: '简体中文',
    antdLocale: () => import('antd/locale/zh_CN'),
  },
  {
    code: 'en',
    label: 'English',
    antdLocale: () => import('antd/locale/en_US'),
  },
]

export const DEFAULT_LOCALE = 'zh-CN'
export const FALLBACK_LOCALE = 'zh-CN'
