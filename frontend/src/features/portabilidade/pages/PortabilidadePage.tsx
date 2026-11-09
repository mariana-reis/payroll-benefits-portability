import { useCallback, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/features/portabilidade/api/http'
import { AgendaEmptyState } from '@/features/portabilidade/components/AgendaEmptyState'
import { AgendaSkeleton } from '@/features/portabilidade/components/AgendaSkeleton'
import { AgendaTable } from '@/features/portabilidade/components/AgendaTable'
import { BeneficiarioSelector } from '@/features/portabilidade/components/BeneficiarioSelector'
import { ErrorBanner } from '@/features/portabilidade/components/ErrorBanner'
import { OptInCard } from '@/features/portabilidade/components/OptInCard'
import { PortabilidadeForm } from '@/features/portabilidade/components/PortabilidadeForm'
import { StatusTracker } from '@/features/portabilidade/components/StatusTracker'
import { Stepper } from '@/features/portabilidade/components/Stepper'
import { useAgenda } from '@/features/portabilidade/hooks/use-agenda'
import { useIdempotencyKey } from '@/features/portabilidade/hooks/use-idempotency-key'
import { useOptIn } from '@/features/portabilidade/hooks/use-opt-in'
import { useSolicitarPortabilidade } from '@/features/portabilidade/hooks/use-solicitar-portabilidade'
import type { Parcela } from '@/features/portabilidade/types/portabilidade'
import { SectionCard } from '@/shared/layouts/SectionCard'
import { Button } from '@/shared/ui/button'
import { ErrorBoundary } from '@/shared/ui/error-boundary'
import { formatCurrency } from '@/shared/utils/format-currency'

function sumSelectedParcels(
  parcelas: Parcela[],
  selectedParcelaIds: ReadonlySet<string>,
): number {
  const totalInCents = parcelas.reduce((total, parcela) => {
    if (parcela.status !== 'ABERTA' || !selectedParcelaIds.has(parcela.parcelaId)) {
      return total
    }
    return total + Math.round(parcela.valor * 100)
  }, 0)

  return totalInCents / 100
}

export function PortabilidadePage() {
  const [beneficiarioId, setBeneficiarioId] = useState<string | null>(null)
  const [autorizadoId, setAutorizadoId] = useState<string | null>(null)
  const [selectedParcelaIds, setSelectedParcelaIds] = useState<ReadonlySet<string>>(
    () => new Set(),
  )
  const [operacaoId, setOperacaoId] = useState<string | null>(null)
  const [isOperationTerminal, setIsOperationTerminal] = useState(false)
  const [lastRequest, setLastRequest] = useState<{
    beneficiarioId: string
    parcelaIds: string[]
    contaDestino: string
  } | null>(null)
  const optIn = useOptIn()
  const agenda = useAgenda(autorizadoId)
  const solicitation = useSolicitarPortabilidade()
  const idempotency = useIdempotencyKey()
  const queryClient = useQueryClient()

  function selectBeneficiario(nextBeneficiarioId: string) {
    setBeneficiarioId(nextBeneficiarioId)
    setAutorizadoId(null)
    setSelectedParcelaIds(new Set())
    setOperacaoId(null)
    setIsOperationTerminal(false)
    setLastRequest(null)
    idempotency.rotate()
    optIn.reset()
    solicitation.reset()
  }

  function authorizeBeneficiario() {
    if (beneficiarioId) {
      optIn.mutate(beneficiarioId, {
        onSuccess: (response) => {
          setAutorizadoId(response.beneficiarioId)
          if (response.beneficiarioId === autorizadoId) {
            void queryClient.invalidateQueries({
              queryKey: ['portabilidade', 'agenda', response.beneficiarioId],
            })
          }
        },
      })
    }
  }

  function changeParcelaSelection(parcelaId: string, selected: boolean) {
    idempotency.rotate()
    setLastRequest(null)
    setSelectedParcelaIds((current) => {
      const next = new Set(current)
      if (selected) {
        next.add(parcelaId)
      } else {
        next.delete(parcelaId)
      }
      return next
    })
  }

  function changeRequestIntent() {
    idempotency.rotate()
    setLastRequest(null)
  }

  function submitRequest(request: NonNullable<typeof lastRequest>) {
    setLastRequest(request)
    solicitation.submit(request, idempotency.key, {
      onSuccess: (response) => {
        setOperacaoId(response.operacaoId)
        setIsOperationTerminal(false)
        setLastRequest(null)
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 409) {
          setAutorizadoId(null)
          setSelectedParcelaIds(new Set())
        }
      },
    })
  }

  function retrySolicitation() {
    if (lastRequest) {
      solicitation.submit(lastRequest, idempotency.key, {
        onSuccess: (response) => {
          setOperacaoId(response.operacaoId)
          setIsOperationTerminal(false)
          setLastRequest(null)
        },
        onError: (error) => {
          if (error instanceof ApiError && error.status === 409) {
            setAutorizadoId(null)
            setSelectedParcelaIds(new Set())
          }
        },
      })
    }
  }

  const handleOperationTerminal = useCallback(() => {
    setIsOperationTerminal(true)
  }, [])

  const error = optIn.error ?? agenda.error
  const isAgendaAuthorizationError =
    agenda.error instanceof ApiError && agenda.error.status === 409
  const operationIsActive = Boolean(operacaoId && !isOperationTerminal)
  const selectedIdsInAgendaOrder =
    agenda.data?.parcelas
      .filter(
        (parcela) =>
          parcela.status === 'ABERTA' && selectedParcelaIds.has(parcela.parcelaId),
      )
      .map((parcela) => parcela.parcelaId) ?? []

  const currentStep = isOperationTerminal
    ? 5
    : operacaoId
      ? 4
      : autorizadoId && autorizadoId === beneficiarioId
        ? selectedIdsInAgendaOrder.length > 0
          ? 3
          : 2
        : 1

  return (
    <ErrorBoundary>
      <div className="space-y-6">
        <Stepper currentStep={currentStep} />

        <BeneficiarioSelector
          selectedBeneficiario={beneficiarioId}
          disabled={optIn.isPending || operationIsActive}
          onSelect={selectBeneficiario}
        />

        <OptInCard
          beneficiarioId={beneficiarioId}
          isAuthorized={Boolean(autorizadoId && autorizadoId === beneficiarioId)}
          isLoading={optIn.isPending}
          onAuthorize={authorizeBeneficiario}
        />

        {error && (
          <ErrorBanner
            error={error}
            actionLabel={
              optIn.error
                ? 'Tentar Opt-In novamente'
                : isAgendaAuthorizationError
                  ? 'Refazer Opt-In'
                  : agenda.isError
                    ? 'Tentar novamente'
                    : undefined
            }
            onAction={
              optIn.error || isAgendaAuthorizationError
                ? authorizeBeneficiario
                : agenda.isError
                  ? () => void agenda.refetch()
                  : undefined
            }
          />
        )}

        {autorizadoId !== null && autorizadoId === beneficiarioId && (
          <SectionCard
            title="Agenda de parcelas"
            description="Selecione apenas parcelas abertas. O total é apenas uma estimativa visual."
          >
            {agenda.isPending && !agenda.data ? (
              <AgendaSkeleton />
            ) : agenda.data?.parcelas.length ? (
              <div className="space-y-4">
                <AgendaTable
                  parcelas={agenda.data.parcelas}
                  selectedParcelaIds={selectedParcelaIds}
                  onSelectionChange={changeParcelaSelection}
                />
                <div className="flex flex-col gap-1 border-t pt-4 text-right">
                  <span className="text-sm text-muted-foreground">Total selecionado</span>
                  <output
                    className="text-lg font-semibold tabular-nums"
                    role="status"
                    aria-label="Total das parcelas selecionadas"
                    aria-live="polite"
                  >
                    {formatCurrency(sumSelectedParcels(agenda.data.parcelas, selectedParcelaIds))}
                  </output>
                </div>
              </div>
            ) : agenda.data ? (
              <AgendaEmptyState />
            ) : null}
          </SectionCard>
        )}

        {solicitation.error && (
          <ErrorBanner
            error={solicitation.error}
            context="solicitation"
            actionLabel={
              solicitation.error instanceof ApiError &&
              (solicitation.error.status === 502 || solicitation.error.status === 503)
                ? 'Reenviar solicitação'
                : solicitation.error instanceof ApiError && solicitation.error.status === 409
                  ? 'Fazer Opt-In novamente'
                  : undefined
            }
            onAction={
              solicitation.error instanceof ApiError && solicitation.error.status === 409
                ? authorizeBeneficiario
                : solicitation.error instanceof ApiError &&
                    (solicitation.error.status === 502 || solicitation.error.status === 503)
                  ? retrySolicitation
                  : undefined
            }
          />
        )}

        {selectedIdsInAgendaOrder.length > 0 &&
          beneficiarioId !== null &&
          autorizadoId === beneficiarioId &&
          (!operacaoId || isOperationTerminal) && (
            <PortabilidadeForm
              beneficiarioId={beneficiarioId}
              parcelaIds={selectedIdsInAgendaOrder}
              isPending={solicitation.isPending}
              onIntentChange={changeRequestIntent}
              onSubmit={submitRequest}
            />
          )}

        {operacaoId && (
          <StatusTracker
            operacaoId={operacaoId}
            onTerminal={handleOperationTerminal}
          />
        )}

        {operacaoId && isOperationTerminal && (
          <div className="flex justify-end">
            <Button
              type="button"
              variant="link"
              onClick={() => {
                setOperacaoId(null)
                setIsOperationTerminal(false)
                idempotency.rotate()
              }}
            >
              Iniciar nova operação
            </Button>
          </div>
        )}
      </div>
    </ErrorBoundary>
  )
}
