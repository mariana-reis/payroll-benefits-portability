import type { EventoResponse } from '@/features/portabilidade/types/portabilidade'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'
import { ScrollArea } from '@/shared/ui/scroll-area'
import { TimelineItem } from './TimelineItem'
import { cn } from 'cn'

export interface EventTimelineProps {
  eventos?: EventoResponse[]
  className?: string
}

export function EventTimeline({ eventos = [], className }: EventTimelineProps) {
  const sortedEventos = [...eventos].sort(
    (a, b) => new Date(a.registradoEm).getTime() - new Date(b.registradoEm).getTime(),
  )

  return (
    <Card className={cn('w-full', className)}>
      <CardHeader>
        <CardTitle className="text-base sm:text-lg">Linha do tempo de eventos</CardTitle>
        <CardDescription>
          Histórico cronológico das ações e integrações desta operação.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {sortedEventos.length === 0 ? (
          <p className="text-sm text-muted-foreground py-2" role="status">
            Nenhum evento registrado até o momento.
          </p>
        ) : (
          <ScrollArea className="max-h-96 pr-3">
            <ol
              className="space-y-1 list-none p-0 m-0"
              aria-label="Histórico de eventos da operação"
            >
              {sortedEventos.map((evento, index) => (
                <TimelineItem
                  key={evento.id}
                  evento={evento}
                  isLast={index === sortedEventos.length - 1}
                />
              ))}
            </ol>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  )
}

