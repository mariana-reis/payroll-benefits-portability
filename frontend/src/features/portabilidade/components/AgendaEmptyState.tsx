import { EmptyState } from '@/shared/ui/empty-state'

export function AgendaEmptyState() {
  return (
    <EmptyState
      title="Nenhuma parcela encontrada"
      description="Não há parcelas disponíveis na agenda deste beneficiário."
    />
  )
}
