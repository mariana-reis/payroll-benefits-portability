import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'

interface OptInCardProps {
  beneficiarioId: string | null
  isAuthorized: boolean
  isLoading: boolean
  onAuthorize: () => void
}

export function OptInCard({
  beneficiarioId,
  isAuthorized,
  isLoading,
  onAuthorize,
}: OptInCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Autorização (Opt-In)</CardTitle>
        <CardDescription>
          Registre a autorização do beneficiário antes de consultar as parcelas.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div aria-live="polite">
          {isAuthorized ? (
            <Badge variant="secondary">Autorização confirmada</Badge>
          ) : (
            <p className="text-sm text-muted-foreground">
              {beneficiarioId
                ? 'A agenda será habilitada após a autorização.'
                : 'Selecione um beneficiário para continuar.'}
            </p>
          )}
        </div>
        <Button
          type="button"
          onClick={onAuthorize}
          disabled={!beneficiarioId || isLoading}
          aria-busy={isLoading}
        >
          {isLoading ? 'Registrando autorização…' : isAuthorized ? 'Atualizar autorização' : 'Realizar Opt-In'}
        </Button>
      </CardContent>
    </Card>
  )
}
