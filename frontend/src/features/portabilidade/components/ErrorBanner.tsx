import { TriangleAlert } from 'lucide-react'
import { ApiError } from '@/features/portabilidade/api/http'
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@/shared/ui/alert'
import { Button } from '@/shared/ui/button'

export interface ErrorBannerProps {
  error: unknown
  actionLabel?: string
  onAction?: () => void
  context?: 'authorization' | 'solicitation' | 'operation'
}

interface BannerMessage {
  title: string
  subtitle?: string
  description: string
}

function getErrorMessage(
  error: unknown,
  context: NonNullable<ErrorBannerProps['context']>,
): BannerMessage {
  if (error instanceof ApiError) {
    if (error.status === 502 || error.status === 503) {
      return {
        title: 'Serviço temporariamente indisponível',
        subtitle: 'Central indisponível',
        description:
          'Não foi possível concluir a solicitação agora. Tente novamente em instantes com a mesma chave de idempotência.',
      }
    }

    if (error.status === 409) {
      if (context === 'solicitation') {
        return {
          title: 'Necessário realizar Opt-In',
          description: 'Faça o Opt-In novamente antes de solicitar a portabilidade.',
        }
      }
      return {
        title: 'Necessário realizar Opt-In',
        description: 'Faça o Opt-In novamente para liberar a consulta da agenda.',
      }
    }

    if (error.status === 404) {
      return {
        title: 'Operação não encontrada',
        description: 'A operação de portabilidade informada não foi encontrada.',
      }
    }

    if (error.status === 400) {
      return {
        title: 'Requisição inválida',
        description: error.message || 'Verifique as informações fornecidas e tente novamente.',
      }
    }

    if (error.status === 422) {
      return {
        title: 'Erro funcional',
        description: error.message || 'Revise os dados e as parcelas selecionadas.',
      }
    }
  }

  return {
    title: 'Não foi possível concluir',
    description: 'Ocorreu um erro inesperado. Tente novamente.',
  }
}

export function ErrorBanner({
  error,
  actionLabel,
  onAction,
  context = 'authorization',
}: ErrorBannerProps) {
  const message = getErrorMessage(error, context)

  return (
    <Alert variant="destructive" className="items-start" role="alert" aria-live="assertive">
      <TriangleAlert aria-hidden="true" />
      <div className="flex-1 space-y-2">
        <AlertTitle className="font-semibold">{message.title}</AlertTitle>
        <AlertDescription className="flex flex-col items-start gap-2">
          {message.subtitle && (
            <span className="font-medium text-foreground">{message.subtitle}</span>
          )}
          <span>{message.description}</span>
          {actionLabel && onAction && (
            <Button type="button" variant="outline" size="sm" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </AlertDescription>
      </div>
    </Alert>
  )
}
