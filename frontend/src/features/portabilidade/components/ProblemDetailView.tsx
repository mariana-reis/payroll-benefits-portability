import { AlertCircle } from 'lucide-react'
import type { ProblemDetail } from '@/features/portabilidade/api/http'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { Badge } from '@/shared/ui/badge'
import { cn } from 'cn'

export interface ProblemDetailViewProps {
  problem: ProblemDetail
  className?: string
}

export function ProblemDetailView({ problem, className }: ProblemDetailViewProps) {
  const title = problem.title || 'Falha na requisição'
  const detail = problem.detail || 'Não foi possível completar a operação solicitada.'
  const status = problem.status

  return (
    <Alert variant="destructive" className={cn('items-start', className)} role="alert">
      <AlertCircle aria-hidden="true" />
      <div className="flex-1 space-y-1">
        <div className="flex items-center gap-2">
          <AlertTitle className="m-0 font-semibold">{title}</AlertTitle>
          {status && (
            <Badge variant="outline" className="text-xs border-destructive/40">
              HTTP {status}
            </Badge>
          )}
        </div>
        <AlertDescription className="text-sm leading-relaxed">
          {detail}
        </AlertDescription>
        {problem.correlationId != null && (
          <p className="text-xs text-muted-foreground mt-2 font-mono">
            ID de correlação: {String(problem.correlationId)}
          </p>
        )}
      </div>
    </Alert>
  )
}

