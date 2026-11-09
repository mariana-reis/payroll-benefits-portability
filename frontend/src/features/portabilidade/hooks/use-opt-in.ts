import { useMutation } from '@tanstack/react-query'
import { ApiError } from '@/features/portabilidade/api/http'
import { registrarOptIn } from '@/features/portabilidade/api/portabilidade-api'
import type { OptInResponse } from '@/features/portabilidade/types/portabilidade'
import { toast } from '@/shared/ui/toast'

export function useOptIn() {
  return useMutation<OptInResponse, Error, string>({
    mutationFn: async (beneficiarioId) => {
      const response = await registrarOptIn(beneficiarioId)
      if (response.beneficiarioId !== beneficiarioId) {
        throw new ApiError(502, {
          type: 'about:blank',
          title: 'Resposta inválida',
          status: 502,
          detail: 'A API retornou uma autorização para outro beneficiário.',
        })
      }

      return response
    },
    retry: false,
    onSuccess: (response) => {
      toast.add({
        type: 'success',
        title: 'Opt-In realizado',
        description: `Autorização registrada para ${response.beneficiarioId}.`,
      })
    },
  })
}
