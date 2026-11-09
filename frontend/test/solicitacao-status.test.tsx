import { act, fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '@/App'
import { LongRunningNotice } from '@/features/portabilidade/components/LongRunningNotice'
import { StatusTracker } from '@/features/portabilidade/components/StatusTracker'
import type { OperacaoResponse } from '@/features/portabilidade/types/portabilidade'
import { renderWithProviders } from './test-utils'
import { server } from './mocks/server'

const API_BASE = 'http://localhost:8080/api/v1/portabilidade'
const operationIds = {
  BEN001: 'b145b246-1c70-4e16-bc20-90104efecb64',
  BEN002: 'b245b246-1c70-4e16-bc20-90104efecb65',
  BEN_ERRO: 'c145b246-1c70-4e16-bc20-90104efecb66',
}

function installSolicitationHandlers(
  statusForBeneficiary: (beneficiaryId: string, requestCount: number) => string,
) {
  let requestCount = 0
  server.use(
    http.post(`${API_BASE}/solicitar`, async ({ request }) => {
      requestCount += 1
      const body = (await request.json()) as { beneficiarioId: string }
      const operacaoId =
        operationIds[body.beneficiarioId as keyof typeof operationIds] ??
        operationIds.BEN001

      return HttpResponse.json(
        {
          operacaoId,
          protocolo: `protocolo-${body.beneficiarioId.toLowerCase()}`,
          status: 'PENDENTE',
          valorTotal: 150,
          criadoEm: new Date().toISOString(),
        },
        { status: 202 },
      )
    }),
    http.get(`${API_BASE}/operacao/:operacaoId`, ({ params }) => {
      const operacaoId = String(params.operacaoId)
      const beneficiaryId =
        Object.entries(operationIds).find(([, id]) => id === operacaoId)?.[0] ??
        'BEN001'
      const status = statusForBeneficiary(beneficiaryId, requestCount)

      return HttpResponse.json({
        id: operacaoId,
        beneficiarioId: beneficiaryId,
        protocoloCentral: `protocolo-${beneficiaryId.toLowerCase()}`,
        status,
        motivoRejeicao:
          status === 'REJEITADA'
            ? 'Beneficiário atingiu o limite de margem consignável'
            : null,
        valorTotal: 150,
        retryAfterSeconds: status === 'PENDENTE' ? 2 : null,
        criadoEm: new Date().toISOString(),
        atualizadoEm: new Date().toISOString(),
        eventos: [
          {
            id: `evt-${operacaoId}-1`,
            tipoEvento: 'OPERACAO_CRIADA',
            statusAnterior: null,
            statusNovo: 'PENDENTE',
            detalhe: 'Operação registrada com sucesso.',
            registradoEm: '2026-11-08T20:30:00Z',
          },
          {
            id: `evt-${operacaoId}-2`,
            tipoEvento:
              status === 'CONFIRMADA'
                ? 'OPERACAO_CONFIRMADA'
                : status === 'REJEITADA'
                  ? 'OPERACAO_REJEITADA'
                  : 'PROTOCOLO_RECEBIDO',
            statusAnterior: 'PENDENTE',
            statusNovo: status,
            detalhe:
              status === 'CONFIRMADA'
                ? 'Operação confirmada pela Central.'
                : status === 'REJEITADA'
                  ? 'Operação rejeitada pela Central.'
                  : 'Protocolo emitido.',
            registradoEm: '2026-11-08T20:31:00Z',
          },
        ],
      })
    }),
  )
}

async function authorizeAndSelect(beneficiarioId: string, parcelaId: string) {
  fireEvent.click(screen.getByRole('button', { name: beneficiarioId }))
  fireEvent.click(screen.getByRole('button', { name: 'Realizar Opt-In' }))
  await screen.findByRole('table', { name: 'Parcelas disponíveis na agenda' })
  fireEvent.click(screen.getByRole('checkbox', { name: `Selecionar parcela ${parcelaId}` }))
  fireEvent.change(screen.getByRole('textbox', { name: 'Conta destino' }), {
    target: { value: 'conta-destino-demo' },
  })
}

afterEach(() => {
  vi.useRealTimers()
})

describe('solicitação e acompanhamento de operação', () => {
  it('solicita BEN001 com os identificadores selecionados, acompanha até CONFIRMADA e exibe timeline e stepper', async () => {
    let requestBody: unknown
    installSolicitationHandlers(() => 'CONFIRMADA')
    server.use(
      http.post(`${API_BASE}/solicitar`, async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json(
          {
            operacaoId: operationIds.BEN001,
            protocolo: 'protocolo-ben001',
            status: 'PENDENTE',
            valorTotal: 150,
            criadoEm: new Date().toISOString(),
          },
          { status: 202 },
        )
      }),
    )

    renderWithProviders(<App />)

    // Initial Stepper step
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Autorização')

    await authorizeAndSelect('BEN001', 'P001')

    // Stepper should reflect Solicitação when parcels are selected
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Solicitação')

    fireEvent.click(screen.getByRole('button', { name: 'Solicitar portabilidade' }))

    // Operation confirms
    expect(await screen.findByText('CONFIRMADA')).toBeInTheDocument()
    expect(requestBody).toEqual({
      beneficiarioId: 'BEN001',
      parcelaIds: ['P001'],
      contaDestino: 'conta-destino-demo',
    })

    // Stepper reaches Conclusão
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Conclusão')

    // Timeline and events are rendered
    expect(screen.getByText('Linha do tempo de eventos')).toBeInTheDocument()
    expect(screen.getByText('OPERACAO_CRIADA')).toBeInTheDocument()
    expect(screen.getByText('OPERACAO_CONFIRMADA')).toBeInTheDocument()

    // Copy protocol button is present
    expect(screen.getByRole('button', { name: 'Copiar protocolo' })).toBeInTheDocument()
  })

  it('exibe o motivo da rejeição de BEN002 e a timeline com os eventos', async () => {
    installSolicitationHandlers(() => 'REJEITADA')
    renderWithProviders(<App />)
    await authorizeAndSelect('BEN002', 'P010')
    fireEvent.click(screen.getByRole('button', { name: 'Solicitar portabilidade' }))

    expect(await screen.findByText('REJEITADA')).toBeInTheDocument()
    expect(
      screen.getByText('Beneficiário atingiu o limite de margem consignável'),
    ).toBeInTheDocument()

    // Timeline displays rejection event
    expect(screen.getByText('OPERACAO_REJEITADA')).toBeInTheDocument()
    expect(screen.getByText('Operação rejeitada pela Central.')).toBeInTheDocument()
  })

  it('retenta BEN_ERRO com a mesma chave e bloqueia submissões duplicadas', async () => {
    let attempts = 0
    const keys: string[] = []
    server.use(
      http.post(`${API_BASE}/opt-in/BEN_ERRO`, () =>
        HttpResponse.json({
          beneficiarioId: 'BEN_ERRO',
          autorizacaoId: 'auth-ben-erro',
          criadoEm: new Date().toISOString(),
        }),
      ),
      http.get(`${API_BASE}/agenda/BEN_ERRO`, () =>
        HttpResponse.json({
          parcelas: [
            {
              parcelaId: 'PERR01',
              convenioId: 'CONV03',
              valor: 75,
              dataDesconto: '2026-04-10',
              status: 'ABERTA',
            },
          ],
        }),
      ),
      http.post(`${API_BASE}/solicitar`, async ({ request }) => {
        attempts += 1
        keys.push(request.headers.get('X-Idempotency-Key') ?? '')
        if (attempts === 1) {
          return HttpResponse.json(
            {
              type: 'about:blank',
              title: 'Central indisponível',
              status: 503,
              detail: 'A Central está temporariamente indisponível.',
              operacaoId: operationIds.BEN_ERRO,
            },
            { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
          )
        }

        return HttpResponse.json(
          {
            operacaoId: operationIds.BEN_ERRO,
            protocolo: 'protocolo-ben-erro',
            status: 'PENDENTE',
            valorTotal: 75,
            criadoEm: new Date().toISOString(),
          },
          { status: 202 },
        )
      }),
      http.get(`${API_BASE}/operacao/${operationIds.BEN_ERRO}`, () =>
        HttpResponse.json({
          id: operationIds.BEN_ERRO,
          beneficiarioId: 'BEN_ERRO',
          protocoloCentral: 'protocolo-ben-erro',
          status: 'CONFIRMADA',
          motivoRejeicao: null,
          valorTotal: 75,
          retryAfterSeconds: null,
          criadoEm: new Date().toISOString(),
          atualizadoEm: new Date().toISOString(),
          eventos: [],
        }),
      ),
    )

    renderWithProviders(<App />)
    await authorizeAndSelect('BEN_ERRO', 'PERR01')
    const submit = screen.getByRole('button', { name: 'Solicitar portabilidade' })
    fireEvent.click(submit)
    fireEvent.click(submit)

    expect(await screen.findByText('Central indisponível')).toBeInTheDocument()
    expect(screen.getByText(/mesma chave de idempotência/i)).toBeInTheDocument()
    expect(attempts).toBe(1)
    fireEvent.click(screen.getByRole('button', { name: 'Reenviar solicitação' }))

    expect(await screen.findByText('CONFIRMADA')).toBeInTheDocument()
    expect(attempts).toBe(2)
    expect(keys[0]).not.toBe('')
    expect(keys[1]).toBe(keys[0])
  })

  it('respeita retryAfterSeconds e atualiza o status sem esperar tempo real', async () => {
    vi.useFakeTimers()
    const operationId = operationIds.BEN001
    const pendingOperation: OperacaoResponse = {
      id: operationId,
      beneficiarioId: 'BEN001',
      protocoloCentral: 'protocolo-ben001',
      status: 'PENDENTE',
      motivoRejeicao: null,
      valorTotal: 150,
      retryAfterSeconds: 5,
      criadoEm: new Date(Date.now()).toISOString(),
      atualizadoEm: new Date(Date.now()).toISOString(),
      eventos: [],
    }
    const confirmedOperation: OperacaoResponse = {
      ...pendingOperation,
      status: 'CONFIRMADA',
      retryAfterSeconds: null,
    }
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 30_000 } },
    })
    queryClient.setQueryData(
      ['portabilidade', 'operacao', operationId],
      pendingOperation,
    )
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(confirmedOperation), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    try {
      render(
        <QueryClientProvider client={queryClient}>
          <StatusTracker operacaoId={operationId} onTerminal={vi.fn()} />
        </QueryClientProvider>,
      )
      expect(screen.getByText('PENDENTE')).toBeInTheDocument()

      await act(async () => {
        await vi.advanceTimersByTimeAsync(4999)
      })
      expect(fetchMock).not.toHaveBeenCalled()

      await act(async () => {
        await vi.advanceTimersByTimeAsync(1)
        await vi.advanceTimersByTimeAsync(1)
        vi.runAllTicks()
        await Promise.resolve()
        await Promise.resolve()
      })
      expect(fetchMock).toHaveBeenCalledOnce()
      expect(screen.getByText('CONFIRMADA')).toBeInTheDocument()
    } finally {
      fetchMock.mockRestore()
      queryClient.clear()
    }
  })

  it('mostra o aviso de longa duração após 60 segundos com mensagem clara', async () => {
    vi.useFakeTimers()
    const criadoEm = new Date(Date.now()).toISOString()
    const onRefresh = vi.fn()
    renderWithProviders(
      <LongRunningNotice criadoEm={criadoEm} onRefresh={onRefresh} isRefreshing={false} />,
    )

    await act(async () => {
      await vi.advanceTimersByTimeAsync(59_999)
    })
    expect(screen.queryByText('Demorando mais que o esperado')).not.toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1)
    })
    expect(screen.getByText('Demorando mais que o esperado')).toBeInTheDocument()
    expect(
      screen.getByText('Esta operação está demorando mais que o esperado.'),
    ).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))
    expect(onRefresh).toHaveBeenCalledOnce()
  })
})
