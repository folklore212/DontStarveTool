import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import '@testing-library/jest-dom'

// Mocks must be before the dynamic import
vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ state: { isAuthenticated: false, userInfo: null }, dispatch: vi.fn() }),
  AuthProvider: ({ children }: any) => children,
}))

vi.mock('../../i18n', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    locale: 'zh-CN',
    setLocale: vi.fn(),
  }),
  I18nProvider: ({ children }: any) => children,
}))

vi.mock('../../hooks/useCaptcha', () => ({
  __esModule: true,
  default: () => ({ loadCaptcha: vi.fn(), resetCaptcha: vi.fn(), captchaRef: { current: vi.fn() } }),
}))

vi.mock('../../hooks/useCaptchaConfig', () => ({
  default: () => ({ loginCaptchaId: 'test-id', registerCaptchaId: 'test-id' }),
}))

vi.mock('../../hooks/useFormError', () => ({
  default: () => ({
    formError: null,
    setFormError: vi.fn(),
    clearFormError: vi.fn(),
  }),
}))

vi.mock('../../utils/tokenManager', () => ({
  tokenManager: {
    getRememberedIdentifier: () => null,
    setRememberedIdentifier: vi.fn(),
    clearRememberedIdentifier: vi.fn(),
    getAccessToken: () => null,
  },
}))

vi.mock('../../api/auth', () => ({
  login: vi.fn().mockResolvedValue({ code: 0, data: { accessToken: 'test', refreshToken: 'test' } }),
}))

vi.mock('../../api/client', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}))

import LoginPage from '../../pages/LoginPage'

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <LoginPage />
    </MemoryRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form without crashing', () => {
    const { container } = renderLogin()
    expect(container).toBeTruthy()
  })

  it('renders username input', () => {
    const { container } = renderLogin()
    // Ant Design Form.Item generates input with id
    const inputs = container.querySelectorAll('input')
    expect(inputs.length).toBeGreaterThanOrEqual(1)
  })

  it('renders login button', () => {
    const { container } = renderLogin()
    const buttons = container.querySelectorAll('button')
    const submitBtn = Array.from(buttons).find(b =>
      b.getAttribute('type') === 'submit'
    )
    expect(submitBtn).toBeTruthy()
  })
})
