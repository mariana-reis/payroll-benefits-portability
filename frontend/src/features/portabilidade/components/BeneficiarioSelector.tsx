import { useState, type SubmitEvent } from 'react'
import { BENEFICIARIOS_DEMO } from '@/shared/constants/beneficiarios'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'

interface BeneficiarioSelectorProps {
  selectedBeneficiario: string | null
  disabled?: boolean
  onSelect: (beneficiarioId: string) => void
}

export function BeneficiarioSelector({
  selectedBeneficiario,
  disabled = false,
  onSelect,
}: BeneficiarioSelectorProps) {
  const [beneficiarioId, setBeneficiarioId] = useState(selectedBeneficiario ?? '')

  function submitCustomBeneficiario(event: SubmitEvent) {
    event.preventDefault()
    const value = beneficiarioId.trim()
    if (value) {
      onSelect(value)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Beneficiário</CardTitle>
        <CardDescription>
          Escolha um cenário de teste ou informe outro identificador.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2" aria-label="Beneficiários de teste">
          {BENEFICIARIOS_DEMO.map((beneficiario) => (
            <Button
              key={beneficiario.id}
              type="button"
              variant={selectedBeneficiario === beneficiario.id ? 'default' : 'outline'}
              aria-pressed={selectedBeneficiario === beneficiario.id}
              disabled={disabled}
              onClick={() => {
                setBeneficiarioId('')
                onSelect(beneficiario.id)
              }}
            >
              {beneficiario.id}
            </Button>
          ))}
        </div>
        <form className="space-y-2" onSubmit={submitCustomBeneficiario}>
          <label className="text-sm font-medium" htmlFor="beneficiario-id">
            Identificador do beneficiário
          </label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input
              id="beneficiario-id"
              value={beneficiarioId}
              onChange={(event) => setBeneficiarioId(event.target.value)}
              placeholder="Informe o identificador"
              autoComplete="off"
              disabled={disabled}
            />
            <Button
              type="submit"
              variant="secondary"
              disabled={disabled || !beneficiarioId.trim()}
            >
              Usar identificador
            </Button>
          </div>
        </form>
        {selectedBeneficiario && (
          <p className="text-sm text-muted-foreground" aria-live="polite">
            Selecionado: <span className="font-medium text-foreground">{selectedBeneficiario}</span>
          </p>
        )}
      </CardContent>
    </Card>
  )
}
