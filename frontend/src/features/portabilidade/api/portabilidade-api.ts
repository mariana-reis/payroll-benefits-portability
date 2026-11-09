import { apiRequest } from '@/features/portabilidade/api/http'
import type {
  AgendaResponse,
  OptInResponse,
  OperacaoResponse,
  SolicitacaoResponse,
  SolicitarPortabilidadeRequest,
} from '@/features/portabilidade/types/portabilidade'

const API_PATH = '/api/v1/portabilidade'

export function registrarOptIn(beneficiarioId: string): Promise<OptInResponse> {
  return apiRequest<OptInResponse>(
    `${API_PATH}/opt-in/${encodeURIComponent(beneficiarioId)}`,
    { method: 'POST' },
  )
}

export function consultarAgenda(beneficiarioId: string): Promise<AgendaResponse> {
  return apiRequest<AgendaResponse>(
    `${API_PATH}/agenda/${encodeURIComponent(beneficiarioId)}`,
  )
}

export function solicitarPortabilidade(
  request: SolicitarPortabilidadeRequest,
  idempotencyKey: string,
): Promise<SolicitacaoResponse> {
  return apiRequest<SolicitacaoResponse>(`${API_PATH}/solicitar`, {
    method: 'POST',
    headers: { 'X-Idempotency-Key': idempotencyKey },
    body: request,
  })
}

export function consultarOperacao(operacaoId: string): Promise<OperacaoResponse> {
  return apiRequest<OperacaoResponse>(
    `${API_PATH}/operacao/${encodeURIComponent(operacaoId)}`,
  )
}
