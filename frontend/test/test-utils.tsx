import type { ReactElement } from 'react'
import { render, type RenderOptions } from '@testing-library/react'
import { AppProviders } from '@/shared/providers/AppProviders'

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>,
) {
  return render(ui, { wrapper: AppProviders, ...options })
}

export * from '@testing-library/react'
