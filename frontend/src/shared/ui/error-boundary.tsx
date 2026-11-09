import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { Button } from '@/shared/ui/button'

export interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
  onReset?: () => void
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = {
    hasError: false,
    error: null,
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary capturou um erro não tratado:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    this.props.onReset?.()
  }

  override render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <Alert variant="destructive" className="items-start my-4" role="alert" aria-live="assertive">
          <AlertCircle aria-hidden="true" />
          <div className="flex-1 space-y-2">
            <AlertTitle className="font-semibold">Erro inesperado na interface</AlertTitle>
            <AlertDescription className="text-sm">
              Ocorreu uma falha ao renderizar esta seção. Tente recarregar a visualização.
            </AlertDescription>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={this.handleReset}
              className="mt-2"
            >
              <RefreshCw className="size-3.5 mr-1.5" aria-hidden="true" />
              Tentar novamente
            </Button>
          </div>
        </Alert>
      )
    }

    return this.props.children
  }
}

