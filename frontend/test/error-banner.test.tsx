import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '@/features/portabilidade/api/http'
import { ErrorBanner } from '@/features/portabilidade/components/ErrorBanner'
import { renderWithProviders } from './test-utils'

describe('ErrorBanner', () => {
  it('shows a retry-friendly message for service unavailable (503)', () => {
    renderWithProviders(
      <ErrorBanner error={new ApiError(503, { status: 503, title: 'Central indisponível' })} />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Central indisponível')
    expect(screen.getByRole('alert')).toHaveTextContent('Serviço temporariamente indisponível')
    expect(screen.getByRole('alert')).toHaveTextContent('Tente novamente em instantes')
    expect(screen.getByRole('alert')).toHaveTextContent('mesma chave de idempotência')
  })

  it('explains that authorization is required for a conflict (409)', () => {
    renderWithProviders(
      <ErrorBanner error={new ApiError(409, { status: 409, title: 'Autorização obrigatória' })} />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Necessário realizar Opt-In')
    expect(screen.getByRole('alert')).toHaveTextContent('Faça o Opt-In novamente')
  })

  it('shows functional error for 422 Unprocessable Entity', () => {
    renderWithProviders(
      <ErrorBanner
        error={
          new ApiError(422, {
            status: 422,
            title: 'Solicitação inválida',
            detail: 'Parcelas informadas não pertencem ao beneficiário.',
          })
        }
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Erro funcional')
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Parcelas informadas não pertencem ao beneficiário.',
    )
  })

  it('shows not found message for 404', () => {
    renderWithProviders(
      <ErrorBanner
        error={
          new ApiError(404, {
            status: 404,
            title: 'Operação não encontrada',
            detail: 'Operação solicitada não foi encontrada.',
          })
        }
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Operação não encontrada')
  })

  it('shows friendly validation message for 400 Bad Request', () => {
    renderWithProviders(
      <ErrorBanner
        error={
          new ApiError(400, {
            status: 400,
            title: 'Requisição inválida',
            detail: 'Parâmetro inválido na solicitação.',
          })
        }
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Requisição inválida')
    expect(screen.getByRole('alert')).toHaveTextContent('Parâmetro inválido na solicitação.')
  })

  it('uses a friendly fallback for unknown errors', () => {
    renderWithProviders(<ErrorBanner error={new Error('internal stack trace')} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível concluir')
    expect(screen.getByRole('alert')).toHaveTextContent('Ocorreu um erro inesperado')
    expect(screen.getByRole('alert')).not.toHaveTextContent('internal stack trace')
  })
})
