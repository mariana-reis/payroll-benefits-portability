import { useCallback } from 'react'
import { apiRequest, type RequestConfig } from '@/features/portabilidade/api/http'

export function useApi() {
  const request = useCallback(
    <TResponse,>(path: string, config?: RequestConfig) =>
      apiRequest<TResponse>(path, config),
    [],
  )

  return { request }
}
