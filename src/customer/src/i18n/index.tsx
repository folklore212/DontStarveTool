import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
  useEffect,
} from 'react'
import { ConfigProvider } from 'antd'
import { tokenManager } from '../utils/tokenManager'
import { interpolate } from './interpolate'
import { SUPPORTED_LOCALES, DEFAULT_LOCALE, FALLBACK_LOCALE } from './config'
import zhCNMessages from './locales/zh-CN'
import enMessages from './locales/en'

type Messages = typeof zhCNMessages
type Namespace = keyof Messages

interface TranslationContextValue {
  locale: string
  setLocale: (code: string) => void
  t: (key: string, params?: Record<string, string | number>) => string
  formatDate: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string
  formatNumber: (value: number, options?: Intl.NumberFormatOptions) => string
}

const TranslationContext = createContext<TranslationContextValue | null>(null)

// Pre-loaded message bundles for fast switching
const MESSAGE_BUNDLES: Record<string, Messages> = {
  'zh-CN': zhCNMessages,
  en: enMessages,
}

const FALLBACK_MESSAGES: Messages = MESSAGE_BUNDLES[FALLBACK_LOCALE] || zhCNMessages

function detectLocale(): string {
  // 1. User-saved preference
  const saved = tokenManager.getLocale()
  if (saved && MESSAGE_BUNDLES[saved]) return saved

  // 2. Browser language
  try {
    const browserLang = navigator.language
    const matched = SUPPORTED_LOCALES.find(
      (l) => browserLang.startsWith(l.code.split('-')[0]),
    )
    if (matched) return matched.code
  } catch {
    // navigator.language not available
  }

  // 3. Default
  return DEFAULT_LOCALE
}

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const initialLocale = detectLocale()
  const [locale, setLocaleState] = useState<string>(initialLocale)
  const [antdLocale, setAntdLocale] = useState<any>(null)
  const [messages, setMessages] = useState<Messages>(
    () => MESSAGE_BUNDLES[initialLocale] || FALLBACK_MESSAGES,
  )

  // Load antd locale on mount
  useEffect(() => {
    const config = SUPPORTED_LOCALES.find((l) => l.code === locale)
    if (config) {
      config.antdLocale().then((mod) => setAntdLocale(mod.default || mod))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const setLocale = useCallback(async (code: string) => {
    const config = SUPPORTED_LOCALES.find((l) => l.code === code)
    if (!config) return

    // Load messages (pre-loaded for zh-CN and en)
    const msgs = MESSAGE_BUNDLES[code] || FALLBACK_MESSAGES

    // Load antd locale
    const antdMod = await config.antdLocale()
    const antd = antdMod.default || antdMod

    setMessages(msgs)
    setAntdLocale(antd)
    setLocaleState(code)
    tokenManager.setLocale(code)
  }, [])

  const t = useCallback(
    (key: string, params?: Record<string, string | number>): string => {
      const parts = key.split('.')
      if (parts.length < 2) {
        // No namespace prefix — search all namespaces
        for (const ns of Object.keys(messages) as Namespace[]) {
          const val = (messages[ns] as Record<string, string>)[key]
          if (typeof val === 'string') return interpolate(val, params)
        }
        return import.meta.env.DEV ? key : ''
      }

      const [nsName, ...rest] = parts
      const msgKey = rest.join('.')
      const ns = messages[nsName as Namespace]

      if (!ns) {
        return import.meta.env.DEV ? key : ''
      }

      const value = (ns as Record<string, string>)[msgKey] as string | undefined
      if (typeof value === 'string') {
        return interpolate(value, params)
      }

      // Fallback to default locale
      const fallbackNs = FALLBACK_MESSAGES[nsName as Namespace]
      if (fallbackNs) {
        const fallbackValue = (fallbackNs as Record<string, string>)[msgKey] as string | undefined
        if (typeof fallbackValue === 'string') return interpolate(fallbackValue, params)
      }

      return import.meta.env.DEV ? key : ''
    },
    [messages],
  )

  const formatDate = useCallback(
    (date: Date | string | number, options?: Intl.DateTimeFormatOptions): string => {
      return new Intl.DateTimeFormat(locale, options).format(new Date(date))
    },
    [locale],
  )

  const formatNumber = useCallback(
    (value: number, options?: Intl.NumberFormatOptions): string => {
      return new Intl.NumberFormat(locale, options).format(value)
    },
    [locale],
  )

  const contextValue = useMemo(
    () => ({ locale, setLocale, t, formatDate, formatNumber }),
    [locale, setLocale, t, formatDate, formatNumber],
  )

  return (
    <ConfigProvider locale={antdLocale}>
      <TranslationContext.Provider value={contextValue}>
        {children}
      </TranslationContext.Provider>
    </ConfigProvider>
  )
}

export function useTranslation(): TranslationContextValue {
  const ctx = useContext(TranslationContext)
  if (!ctx) {
    throw new Error('useTranslation must be used within a LocaleProvider')
  }
  return ctx
}
