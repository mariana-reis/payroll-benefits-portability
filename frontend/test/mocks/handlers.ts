import { http, HttpResponse, type RequestHandler } from 'msw'

const API_BASE = 'http://localhost:8080/api/v1/portabilidade'

const agendas = {
  BEN001: [
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
      valor: 150,
      dataDesconto: '2026-02-10',
      status: 'ABERTA',
    },
  ],
  BEN002: [
    {
      parcelaId: 'P010',
      convenioId: 'CONV02',
      valor: 300,
      dataDesconto: '2026-01-15',
      status: 'ABERTA',
    },
  ],
} as const

export const handlers: RequestHandler[] = [
  http.post(`${API_BASE}/opt-in/:beneficiarioId`, ({ params }) => {
    const beneficiarioId = String(params.beneficiarioId)
    if (beneficiarioId === 'BEN_ERRO') {
      return HttpResponse.json(
        {
          type: 'about:blank',
          title: 'Central indisponível',
          status: 503,
          detail: 'A Central está temporariamente indisponível.',
        },
        { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
      )
    }

    return HttpResponse.json(
      {
        beneficiarioId,
        autorizacaoId: `auth-${beneficiarioId.toLowerCase()}`,
        criadoEm: '2026-11-08T20:35:00Z',
      },
      { status: 200 },
    )
  }),
  http.get(`${API_BASE}/agenda/:beneficiarioId`, ({ params }) => {
    const beneficiarioId = String(params.beneficiarioId)
    if (beneficiarioId === 'BEN001') {
      return HttpResponse.json({ parcelas: agendas.BEN001 })
    }
    if (beneficiarioId === 'BEN002') {
      return HttpResponse.json({ parcelas: agendas.BEN002 })
    }
    return HttpResponse.json({ parcelas: [] })
  }),
]
