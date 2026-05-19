import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { ConfigProvider, theme as antdTheme } from 'antd'

interface ThemeContextType {
  darkMode: boolean
  toggleDarkMode: () => void
}

const ThemeContext = createContext<ThemeContextType>({ darkMode: false, toggleDarkMode: () => {} })

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark')

  useEffect(() => {
    localStorage.setItem('theme', darkMode ? 'dark' : 'light')
    document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light')
  }, [darkMode])

  const toggleDarkMode = useCallback(() => setDarkMode((v) => !v), [])

  return (
    <ThemeContext.Provider value={{ darkMode, toggleDarkMode }}>
      <ConfigProvider theme={{ algorithm: darkMode ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm }}>
        {children}
      </ConfigProvider>
    </ThemeContext.Provider>
  )
}

export function useTheme() { return useContext(ThemeContext) }
