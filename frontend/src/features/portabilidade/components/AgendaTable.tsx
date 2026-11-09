import { formatCurrency } from '@/shared/utils/format-currency'
import { formatDate } from '@/shared/utils/format-date'
import { Badge } from '@/shared/ui/badge'
import type { Parcela } from '@/features/portabilidade/types/portabilidade'

interface AgendaTableProps {
  parcelas: Parcela[]
  selectedParcelaIds: ReadonlySet<string>
  onSelectionChange: (parcelaId: string, selected: boolean) => void
}

export function AgendaTable({
  parcelas,
  selectedParcelaIds,
  onSelectionChange,
}: AgendaTableProps) {
  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full min-w-2xl text-left text-sm">
        <caption className="sr-only">Parcelas disponíveis na agenda</caption>
        <thead className="bg-muted/60 text-muted-foreground">
          <tr>
            <th scope="col" className="w-12 px-4 py-3">
              <span className="sr-only">Selecionar</span>
            </th>
            <th scope="col" className="px-4 py-3 font-medium">Parcela</th>
            <th scope="col" className="px-4 py-3 font-medium">Valor</th>
            <th scope="col" className="px-4 py-3 font-medium">Data do desconto</th>
            <th scope="col" className="px-4 py-3 font-medium">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {parcelas.map((parcela) => {
            const isOpen = parcela.status === 'ABERTA'
            const isSelected = selectedParcelaIds.has(parcela.parcelaId)

            return (
              <tr key={parcela.parcelaId}>
                <td className="px-4 py-3">
                  <input
                    type="checkbox"
                    aria-label={`Selecionar parcela ${parcela.parcelaId}`}
                    checked={isSelected}
                    disabled={!isOpen}
                    onChange={(event) =>
                      onSelectionChange(parcela.parcelaId, event.target.checked)
                    }
                    className="size-4 accent-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-not-allowed"
                  />
                </td>
                <th scope="row" className="px-4 py-3 font-medium">
                  {parcela.parcelaId}
                </th>
                <td className="px-4 py-3 tabular-nums">{formatCurrency(parcela.valor)}</td>
                <td className="px-4 py-3">{formatDate(parcela.dataDesconto)}</td>
                <td className="px-4 py-3">
                  <Badge variant={isOpen ? 'secondary' : 'outline'}>{parcela.status}</Badge>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
