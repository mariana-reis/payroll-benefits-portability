import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import type { SolicitarPortabilidadeRequest } from '@/features/portabilidade/types/portabilidade'

const contaDestinoSchema = z.object({
  contaDestino: z
    .string()
    .trim()
    .min(1, 'Informe a conta de destino.')
    .max(255, 'A conta de destino deve ter no máximo 255 caracteres.'),
})

type ContaDestinoForm = z.infer<typeof contaDestinoSchema>

interface PortabilidadeFormProps {
  beneficiarioId: string
  parcelaIds: string[]
  isPending: boolean
  onIntentChange: () => void
  onSubmit: (request: SolicitarPortabilidadeRequest) => void
}

export function PortabilidadeForm({
  beneficiarioId,
  parcelaIds,
  isPending,
  onIntentChange,
  onSubmit,
}: PortabilidadeFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ContaDestinoForm>({
    resolver: zodResolver(contaDestinoSchema),
    defaultValues: { contaDestino: '' },
  })

  function submit({ contaDestino }: ContaDestinoForm) {
    onSubmit({ beneficiarioId, parcelaIds, contaDestino })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Solicitar portabilidade</CardTitle>
        <CardDescription>
          Informe a conta de destino e confirme as parcelas selecionadas.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={handleSubmit(submit)} noValidate>
          <div className="space-y-2">
            <label htmlFor="conta-destino" className="text-sm font-medium">
              Conta destino
            </label>
            <Input
              id="conta-destino"
              autoComplete="off"
              aria-invalid={Boolean(errors.contaDestino)}
              aria-describedby={errors.contaDestino ? 'conta-destino-error' : undefined}
              disabled={isPending}
              {...register('contaDestino', { onChange: onIntentChange })}
            />
            {errors.contaDestino && (
              <p id="conta-destino-error" className="text-sm text-destructive" role="alert">
                {errors.contaDestino.message}
              </p>
            )}
          </div>
          <p className="text-sm text-muted-foreground" aria-live="polite">
            {parcelaIds.length} {parcelaIds.length === 1 ? 'parcela selecionada' : 'parcelas selecionadas'}
          </p>
          <Button type="submit" disabled={isPending || parcelaIds.length === 0} aria-busy={isPending}>
            {isPending ? 'Enviando solicitação…' : 'Solicitar portabilidade'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
