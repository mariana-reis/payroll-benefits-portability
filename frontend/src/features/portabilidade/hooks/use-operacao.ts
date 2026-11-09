import { useQuery } from '@tanstack/react-query'
import { ApiError } from '@/features/portabilidade/api/http'
import { consultarOperacao } from '@/features/portabilidade/api/portabilidade-api'

export function useOperacao(operacaoId: string | null) {
  return useQuery({
    queryKey: ['portabilidade', 'operacao', operacaoId],
    queryFn: () => {
      if (!operacaoId) {
        throw new Error('É necessário informar o identificador da operação.')
      }
      return consultarOperacao(operacaoId)
    },
    enabled: Boolean(operacaoId),
    retry: (failureCount, error) =>
      failureCount < 1 && (!(error instanceof ApiError) || error.status >= 500),
  })
}
