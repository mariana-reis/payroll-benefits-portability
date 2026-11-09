import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '@/App'
import { renderWithProviders } from './test-utils'

describe('App', () => {
  it('renders the application shell', () => {
    renderWithProviders(<App />)

    expect(
      screen.getByRole('heading', { name: 'Portabilidade de Convênios' }),
    ).toBeInTheDocument()
  })
})
