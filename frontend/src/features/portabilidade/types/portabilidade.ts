export interface OptInResponse {
  beneficiarioId: string
  autorizacaoId: string
  criadoEm: string
}

export interface Parcela {
  parcelaId: string
  convenioId: string
  valor: number
  dataDesconto: string
  status: string
}

export interface AgendaResponse {
  parcelas: Parcela[]
}

export type StatusOperacao =
  | 'PENDENTE'
  | 'CONFIRMADA'
  | 'REJEITADA'
  | 'FALHA_INTEGRACAO'

export interface SolicitarPortabilidadeRequest {
  beneficiarioId: string
  parcelaIds: string[]
  contaDestino: string
}

export interface SolicitacaoResponse {
  operacaoId: string
  protocolo: string
  status: StatusOperacao
  valorTotal: number
  criadoEm: string
}

export interface EventoResponse {
  id: string
  tipoEvento: string
  statusAnterior: StatusOperacao | null
  statusNovo: StatusOperacao | null
  detalhe: string
  registradoEm: string
}

export interface OperacaoResponse {
  id: string
  beneficiarioId: string
  protocoloCentral: string | null
  status: StatusOperacao
  motivoRejeicao: string | null
  valorTotal: number
  retryAfterSeconds: number | null
  criadoEm: string
  atualizadoEm: string
  eventos: EventoResponse[]
}
