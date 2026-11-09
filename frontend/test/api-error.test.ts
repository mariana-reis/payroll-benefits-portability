import { describe, expect, it } from 'vitest'
import { ApiError } from '@/features/portabilidade/api/http'

describe('ApiError', () => {
  it('preserves the HTTP status and Problem Details document', () => {
    const error = new ApiError(422, {
      type: 'about:blank',
      title: 'Solicitação inválida',
      status: 422,
      detail: 'O payload não passou na validação.',
    })

    expect(error).toBeInstanceOf(Error)
    expect(error.name).toBe('ApiError')
    expect(error.status).toBe(422)
    expect(error.message).toBe('O payload não passou na validação.')
    expect(error.problemDetail.status).toBe(422)
  })
})
