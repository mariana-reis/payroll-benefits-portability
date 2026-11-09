import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { Button } from '@/shared/ui/button'
import { cn } from 'cn'

export interface RetryBannerProps {
  onRetry: () => void
  isRetrying?: boolean
  title?: string
  description?: string
  className?: string
}

export function RetryBanner({
  onRetry,
  isRetrying = false,
  title = 'Falha de integração',
  description = 'A Central de Convênios está temporariamente indisponível. A operação foi salva e pode ser reenviada com segurança com a mesma chave de idempotência.',
  className,
}: RetryBannerProps) {
  return (
    <Alert
      variant="destructive"
      className={cn('items-start', className)}
      role="alert"
      aria-live="assertive"
    >
      <AlertTriangle aria-hidden="true" />
      <div className="flex-1 space-y-2">
        <AlertTitle className="font-semibold">{title}</AlertTitle>
        <AlertDescription className="text-sm leading-relaxed">
          {description}
        </AlertDescription>
        <div className="pt-1">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onRetry}
            disabled={isRetrying}
            className="border-destructive/40 hover:bg-destructive/10"
          >
            <RefreshCw
              className={cn('size-3.5 mr-1.5', isRetrying && 'animate-spin')}
              aria-hidden="true"
            />
            {isRetrying ? 'Reenviando…' : 'Tentar novamente'}
          </Button>
        </div>
      </div>
    </Alert>
  )
}

