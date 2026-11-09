import { CheckCircle2, AlertTriangle, FileText, RefreshCw, XCircle } from 'lucide-react'
import type { EventoResponse } from '@/features/portabilidade/types/portabilidade'
import { Badge } from '@/shared/ui/badge'
import { formatDate, formatTime } from '@/shared/utils/format-date'

export interface TimelineItemProps {
  evento: EventoResponse
  isLast?: boolean
}

function getEventBadgeVariant(tipo: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (tipo) {
    case 'OPERACAO_CONFIRMADA':
      return 'secondary'
    case 'OPERACAO_REJEITADA':
    case 'OPERACAO_FALHA_INTEGRACAO':
      return 'destructive'
    case 'OPERACAO_CRIADA':
    case 'PROTOCOLO_RECEBIDO':
    case 'OPERACAO_REPROCESSADA':
    default:
      return 'outline'
  }
}

function getEventIcon(tipo: string) {
  switch (tipo) {
    case 'OPERACAO_CONFIRMADA':
      return <CheckCircle2 className="size-4 text-emerald-600" aria-hidden="true" />
    case 'OPERACAO_REJEITADA':
      return <XCircle className="size-4 text-destructive" aria-hidden="true" />
    case 'OPERACAO_FALHA_INTEGRACAO':
      return <AlertTriangle className="size-4 text-amber-600" aria-hidden="true" />
    case 'OPERACAO_REPROCESSADA':
      return <RefreshCw className="size-4 text-blue-600" aria-hidden="true" />
    case 'PROTOCOLO_RECEBIDO':
    case 'OPERACAO_CRIADA':
    default:
      return <FileText className="size-4 text-muted-foreground" aria-hidden="true" />
  }
}

export function TimelineItem({ evento, isLast }: TimelineItemProps) {
  const data = formatDate(evento.registradoEm)
  const hora = formatTime(evento.registradoEm)

  return (
    <li className="relative flex gap-3 pb-6 group last:pb-0" data-testid="timeline-item">
      {!isLast && (
        <span
          className="absolute left-3.5 top-6 bottom-0 w-0.5 bg-border -translate-x-1/2"
          aria-hidden="true"
        />
      )}
      <div className="flex size-7 shrink-0 items-center justify-center rounded-full border bg-background shadow-xs">
        {getEventIcon(evento.tipoEvento)}
      </div>
      <div className="flex-1 space-y-1.5">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant={getEventBadgeVariant(evento.tipoEvento)}>
            {evento.tipoEvento}
          </Badge>
          <time
            dateTime={evento.registradoEm}
            className="text-xs text-muted-foreground tabular-nums"
          >
            <span className="font-medium text-foreground/80">{data}</span> às <span>{hora}</span>
          </time>
        </div>

        <p className="text-sm text-foreground/90 leading-relaxed">{evento.detalhe}</p>

        {(evento.statusAnterior || evento.statusNovo) && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground pt-0.5">
            <span className="font-medium text-muted-foreground/80">Transição:</span>
            <span className="font-semibold text-foreground">
              {`${evento.statusAnterior ?? '—'} → ${evento.statusNovo ?? '—'}`}
            </span>
          </div>
        )}
      </div>
    </li>
  )
}
