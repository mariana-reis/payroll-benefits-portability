import { useQuery } from '@tanstack/react-query'
import { consultarAgenda } from '@/features/portabilidade/api/portabilidade-api'

export function useAgenda(beneficiarioId: string | null) {
  return useQuery({
    queryKey: ['portabilidade', 'agenda', beneficiarioId],
    queryFn: () => {
      if (!beneficiarioId) {
        throw new Error('É necessário informar um beneficiário autorizado.')
      }

      return consultarAgenda(beneficiarioId)
    },
    enabled: Boolean(beneficiarioId),
    retry: (failureCount, error) => {
      if (error instanceof Error && 'status' in error && error.status === 409) {
        return false
      }
      return failureCount < 1
    },
  })
}
