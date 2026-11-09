import { useEffect, useState } from 'react'
import { Clock3 } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { Button } from '@/shared/ui/button'

export interface LongRunningNoticeProps {
  criadoEm: string
  onRefresh: () => void
  isRefreshing: boolean
}

export function LongRunningNotice({
  criadoEm,
  onRefresh,
  isRefreshing,
}: LongRunningNoticeProps) {
  const [isLongRunning, setIsLongRunning] = useState(false)

  useEffect(() => {
    const createdAt = Date.parse(criadoEm)
    const elapsed = Number.isNaN(createdAt) ? 0 : Date.now() - createdAt
    const delay = Math.max(0, 60_000 - elapsed)
    const timeoutId = window.setTimeout(() => setIsLongRunning(true), delay)

    return () => window.clearTimeout(timeoutId)
  }, [criadoEm])

  if (!isLongRunning) {
    return null
  }

  return (
    <Alert className="items-start" role="status" aria-live="polite">
      <Clock3 aria-hidden="true" />
      <AlertTitle>Demorando mais que o esperado</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-3">
        <span>Esta operação está demorando mais que o esperado.</span>
        <Button type="button" size="sm" variant="outline" onClick={onRefresh} disabled={isRefreshing}>
          {isRefreshing ? 'Atualizando…' : 'Atualizar'}
        </Button>
      </AlertDescription>
    </Alert>
  )
}
