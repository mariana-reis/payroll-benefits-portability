import { useEffect, useRef } from 'react'
import { RefreshCw, CheckCircle2 } from 'lucide-react'
import { CopyProtocolButton } from '@/features/portabilidade/components/CopyProtocolButton'
import { ErrorBanner } from '@/features/portabilidade/components/ErrorBanner'
import { EventTimeline } from '@/features/portabilidade/components/EventTimeline'
import { LongRunningNotice } from '@/features/portabilidade/components/LongRunningNotice'
import { RejectionCard } from '@/features/portabilidade/components/RejectionCard'
import { RetryBanner } from '@/features/portabilidade/components/RetryBanner'
import { useOperacaoPolling } from '@/features/portabilidade/hooks/use-operacao-polling'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Skeleton } from '@/shared/ui/skeleton'
import { toast } from '@/shared/ui/toast'
import type { StatusOperacao } from '@/features/portabilidade/types/portabilidade'

export interface StatusTrackerProps {
  operacaoId: string
  onTerminal: () => void
}

function getStatusVariant(status: StatusOperacao) {
  if (status === 'REJEITADA' || status === 'FALHA_INTEGRACAO') {
    return 'destructive'
  }
  if (status === 'CONFIRMADA') {
    return 'secondary'
  }
  return 'outline'
}

function safeToast(options: { title: string; description?: string; type?: string }) {
  try {
    toast.create?.(options)
  } catch {
    // safe fallback
  }
}

export function StatusTracker({ operacaoId, onTerminal }: StatusTrackerProps) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const confirmedRef = useRef<HTMLDivElement>(null)
  const rejectionRef = useRef<HTMLParagraphElement>(null)
  const previousStatusRef = useRef<StatusOperacao | null>(null)

  const operacao = useOperacaoPolling(operacaoId)
  const data = operacao.data
  const isTerminal = data?.status === 'CONFIRMADA' || data?.status === 'REJEITADA'

  useEffect(() => {
    headingRef.current?.focus()
  }, [operacaoId])

  useEffect(() => {
    if (isTerminal) {
      onTerminal()
    }
  }, [isTerminal, onTerminal])

  // Focus management and toasts on status transitions
  useEffect(() => {
    if (!data?.status) return

    if (previousStatusRef.current !== data.status) {
      if (data.status === 'CONFIRMADA') {
        safeToast({
          title: 'Status confirmado',
          description: 'A operação de portabilidade foi confirmada com sucesso.',
          type: 'success',
        })
        setTimeout(() => {
          confirmedRef.current?.focus()
        }, 50)
      } else if (data.status === 'REJEITADA') {
        safeToast({
          title: 'Operação rejeitada',
          description: 'A operação de portabilidade foi rejeitada pela Central.',
          type: 'error',
        })
        setTimeout(() => {
          rejectionRef.current?.focus()
        }, 50)
      }
      previousStatusRef.current = data.status
    }
  }, [data?.status, data?.motivoRejeicao])

  async function handleManualRefresh() {
    await operacao.refetch()
    safeToast({
      title: 'Atualização concluída',
      type: 'info',
    })
  }

  return (
    <Card className="w-full">
      <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <CardTitle>
          <h2
            ref={headingRef}
            tabIndex={-1}
            className="outline-none focus-visible:ring-2 focus-visible:ring-ring text-lg font-semibold"
          >
            Acompanhamento da operação
          </h2>
        </CardTitle>
        <div className="flex flex-wrap items-center gap-2">
          {data && (
            <Badge variant={getStatusVariant(data.status)} aria-live="polite">
              {data.status}
            </Badge>
          )}
          {data?.protocoloCentral && (
            <CopyProtocolButton protocolo={data.protocoloCentral} />
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        {operacao.isPending && !data && (
          <div className="space-y-2" aria-label="Carregando status da operação">
            <Skeleton className="h-5 w-36" />
            <Skeleton className="h-4 w-full max-w-md" />
          </div>
        )}

        {operacao.error && (
          <ErrorBanner
            error={operacao.error}
            context="operation"
            actionLabel="Atualizar status"
            onAction={handleManualRefresh}
          />
        )}

        {data && (
          <div aria-live="polite" className="space-y-6">
            {data.status === 'CONFIRMADA' && (
              <div
                ref={confirmedRef}
                tabIndex={-1}
                role="status"
                aria-label="Operação confirmada com sucesso"
                className="flex items-start gap-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-emerald-900 dark:text-emerald-200 outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <CheckCircle2 className="size-5 shrink-0 text-emerald-600 mt-0.5" aria-hidden="true" />
                <div className="space-y-1">
                  <h3 className="font-semibold text-base">Operação Confirmada</h3>
                  <p className="text-sm">
                    A transferência do convênio foi concluída e aprovada pela Central.
                  </p>
                  {data.protocoloCentral && (
                    <div className="pt-2 flex items-center gap-2">
                      <span className="text-xs font-mono bg-background/80 px-2 py-1 rounded border">
                        Protocolo: {data.protocoloCentral}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {data.status === 'REJEITADA' && (
              <RejectionCard
                motivoRejeicao={data.motivoRejeicao}
                contentRef={rejectionRef}
              />
            )}

            {data.status === 'FALHA_INTEGRACAO' && (
              <RetryBanner
                title="Falha de integração na Central"
                description="Houve uma falha na integração com a Central de Convênios. A operação está preservada e pode ser consultada ou reenviada."
                onRetry={handleManualRefresh}
                isRetrying={operacao.isFetching}
              />
            )}

            {data.status === 'PENDENTE' && (
              <LongRunningNotice
                criadoEm={data.criadoEm}
                onRefresh={handleManualRefresh}
                isRefreshing={operacao.isFetching}
              />
            )}

            {!isTerminal && (
              <Button
                type="button"
                variant="outline"
                onClick={handleManualRefresh}
                disabled={operacao.isFetching}
              >
                <RefreshCw
                  className={operacao.isFetching ? 'animate-spin' : ''}
                  aria-hidden="true"
                />
                {operacao.isFetching ? 'Atualizando status…' : 'Atualizar status'}
              </Button>
            )}

            <EventTimeline eventos={data.eventos} />
          </div>
        )}
      </CardContent>
    </Card>
  )
}
