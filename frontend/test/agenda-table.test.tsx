import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AgendaTable } from '@/features/portabilidade/components/AgendaTable'
import type { Parcela } from '@/features/portabilidade/types/portabilidade'
import { renderWithProviders } from './test-utils'

const parcelas: Parcela[] = [
  {
    parcelaId: 'P001',
    convenioId: 'CONV01',
    valor: 150,
    dataDesconto: '2026-01-10',
    status: 'ABERTA',
  },
  {
    parcelaId: 'P002',
    convenioId: 'CONV01',
    valor: 10,
    dataDesconto: '2026-02-10',
    status: 'PAGA',
  },
]

describe('AgendaTable', () => {
  it('renders parcel details and allows selection only for open parcels', () => {
    const onSelectionChange = vi.fn()
    renderWithProviders(
      <AgendaTable
        parcelas={parcelas}
        selectedParcelaIds={new Set()}
        onSelectionChange={onSelectionChange}
      />,
    )

    expect(screen.getByRole('cell', { name: 'R$ 150,00' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '10/01/2026' })).toBeInTheDocument()

    const openParcel = screen.getByRole('checkbox', { name: 'Selecionar parcela P001' })
    const closedParcel = screen.getByRole('checkbox', { name: 'Selecionar parcela P002' })
    expect(openParcel).toBeEnabled()
    expect(closedParcel).toBeDisabled()

    fireEvent.click(openParcel)
    expect(onSelectionChange).toHaveBeenCalledWith('P001', true)
  })
})
