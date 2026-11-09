import { useCallback, useRef } from 'react'
import { useMutation } from '@tanstack/react-query'
import { solicitarPortabilidade } from '@/features/portabilidade/api/portabilidade-api'
import type {
  SolicitacaoResponse,
  SolicitarPortabilidadeRequest,
} from '@/features/portabilidade/types/portabilidade'

interface SolicitationVariables {
  request: SolicitarPortabilidadeRequest
  idempotencyKey: string
}

interface SubmitCallbacks {
  onSuccess?: (response: SolicitacaoResponse) => void
  onError?: (error: Error) => void
}

export function useSolicitarPortabilidade() {
  const inFlight = useRef(false)
  const mutation = useMutation({
    mutationFn: ({ request, idempotencyKey }: SolicitationVariables) =>
      solicitarPortabilidade(request, idempotencyKey),
    retry: false,
  })

  const submit = useCallback(
    (
      request: SolicitarPortabilidadeRequest,
      idempotencyKey: string,
      callbacks: SubmitCallbacks = {},
    ) => {
      if (inFlight.current) {
        return false
      }

      inFlight.current = true
      mutation.mutate(
        { request, idempotencyKey },
        {
          onSuccess: callbacks.onSuccess,
          onError: callbacks.onError,
          onSettled: () => {
            inFlight.current = false
          },
        },
      )
      return true
    },
    [mutation],
  )

  return { ...mutation, submit }
}
