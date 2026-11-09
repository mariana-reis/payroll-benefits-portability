import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { BeneficiarioSelector } from '@/features/portabilidade/components/BeneficiarioSelector'
import { renderWithProviders } from './test-utils'

describe('BeneficiarioSelector', () => {
  it('selects one of the predefined beneficiaries', () => {
    const onSelect = vi.fn()
    renderWithProviders(
      <BeneficiarioSelector
        selectedBeneficiario={null}
        onSelect={onSelect}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'BEN002' }))

    expect(onSelect).toHaveBeenCalledWith('BEN002')
  })

  it('accepts a custom beneficiary identifier', () => {
    const onSelect = vi.fn()
    renderWithProviders(
      <BeneficiarioSelector
        selectedBeneficiario={null}
        onSelect={onSelect}
      />,
    )

    fireEvent.change(screen.getByRole('textbox', { name: 'Identificador do beneficiário' }), {
      target: { value: ' BEN_CUSTOM ' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Usar identificador' }))

    expect(onSelect).toHaveBeenCalledWith('BEN_CUSTOM')
  })
})
