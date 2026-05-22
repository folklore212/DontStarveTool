import { describe, it, expect, vi } from 'vitest'
import { render } from '@testing-library/react'
import '@testing-library/jest-dom'

// Minimal mocks for lightweight components
vi.mock('../../i18n', () => ({
  useTranslation: () => ({ t: (key: string) => key, locale: 'zh-CN', setLocale: vi.fn() }),
}))

import ErrorAlert from '../../components/ErrorAlert'

describe('ErrorAlert', () => {
  it('renders without crashing', () => {
    const { container } = render(<ErrorAlert message="test error" />)
    expect(container).toBeTruthy()
    expect(container.textContent).toContain('test error')
  })

  it('renders nothing when no message', () => {
    const { container } = render(<ErrorAlert message="" />)
    expect(container.querySelector('.ant-alert')).toBeFalsy()
  })
})
