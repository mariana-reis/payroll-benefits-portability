import { fireEvent, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '@/App'
import { renderWithProviders } from './test-utils'
import { server } from './mocks/server'

function chooseBeneficiary(beneficiarioId: string) {
  fireEvent.click(screen.getByRole('button', { name: beneficiarioId }))
  fireEvent.click(screen.getByRole('button', { name: 'Realizar Opt-In' }))
}

describe('Opt-In and agenda flow', () => {
  it('authorizes BEN001, loads two parcels and totals the selected ones', async () => {
    renderWithProviders(<App />)
    chooseBeneficiary('BEN001')

    expect(await screen.findByText('Autorização registrada para BEN001.')).toBeInTheDocument()
    const table = await screen.findByRole('table', { name: 'Parcelas disponíveis na agenda' })
    expect(within(table).getByRole('row', { name: /P001/ })).toBeInTheDocument()
    expect(within(table).getByRole('row', { name: /P002/ })).toBeInTheDocument()
    expect(screen.getByRole('status', { name: 'Total das parcelas selecionadas' })).toHaveTextContent(/R\$\s0,00/)

    fireEvent.click(screen.getByRole('checkbox', { name: 'Selecionar parcela P001' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Selecionar parcela P002' }))

    expect(screen.getByRole('status', { name: 'Total das parcelas selecionadas' })).toHaveTextContent(/R\$\s300,00/)
  })

  it('authorizes BEN002 and loads its single parcel', async () => {
    renderWithProviders(<App />)
    chooseBeneficiary('BEN002')

    const table = await screen.findByRole('table', { name: 'Parcelas disponíveis na agenda' })
    expect(within(table).getByRole('row', { name: /P010/ })).toBeInTheDocument()
    expect(within(table).getAllByRole('row')).toHaveLength(2)
    expect(screen.getByRole('checkbox', { name: 'Selecionar parcela P010' })).toBeEnabled()
  })

  it('shows the 503 banner for BEN_ERRO and does not request the agenda', async () => {
    renderWithProviders(<App />)
    chooseBeneficiary('BEN_ERRO')

    expect(
      await screen.findByText('Serviço temporariamente indisponível'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Parcelas disponíveis na agenda' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar Opt-In novamente' })).toBeEnabled()
    expect(screen.queryByText('Autorização confirmada')).not.toBeInTheDocument()
  })

  it('shows an empty state when an authorized beneficiary has no parcels', async () => {
    renderWithProviders(<App />)
    fireEvent.change(screen.getByRole('textbox', { name: /Identificador do beneficiário/ }), {
      target: { value: 'BEN099' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Usar identificador' }))
    fireEvent.click(screen.getByRole('button', { name: 'Realizar Opt-In' }))

    expect(await screen.findByText('Nenhuma parcela encontrada')).toBeInTheDocument()
  })

  it('offers re-authorization when the agenda API returns 409', async () => {
    let agendaRequests = 0
    server.use(
      http.get(
        'http://localhost:8080/api/v1/portabilidade/agenda/BEN099',
        () => {
          agendaRequests += 1
          if (agendaRequests === 1) {
            return HttpResponse.json(
              {
                type: 'about:blank',
                title: 'Autorização obrigatória',
                status: 409,
                detail: 'O beneficiário precisa autorizar.',
              },
              { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
            )
          }

          return HttpResponse.json({ parcelas: [] })
        },
      ),
    )
    renderWithProviders(<App />)
    fireEvent.change(screen.getByRole('textbox', { name: /Identificador do beneficiário/ }), {
      target: { value: 'BEN099' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Usar identificador' }))
    fireEvent.click(screen.getByRole('button', { name: 'Realizar Opt-In' }))

    expect(await screen.findByText('Faça o Opt-In novamente para liberar a consulta da agenda.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Refazer Opt-In' }))
    expect(await screen.findByText('Nenhuma parcela encontrada')).toBeInTheDocument()
  })
})
