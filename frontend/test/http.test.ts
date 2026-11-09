import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '@/features/portabilidade/api/http'
import { server } from './mocks/server'

const problemStatuses = [400, 404, 409, 422, 503]

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it.each(problemStatuses)('parses Problem Details for HTTP %s', async (status) => {
    vi.stubEnv('VITE_API_URL', 'http://localhost:8080')
    server.use(
      http.get('http://localhost:8080/problems', () =>
        HttpResponse.json(
          {
            type: 'about:blank',
            title: `Erro ${status}`,
            status,
            detail: 'A solicitação não foi concluída.',
          },
          {
            status,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )

    await expect(apiRequest('/problems')).rejects.toMatchObject({
      name: 'ApiError',
      status,
      problemDetail: {
        title: `Erro ${status}`,
        status,
        detail: 'A solicitação não foi concluída.',
      },
    })
  })
})
