import { Skeleton } from '@/shared/ui/skeleton'

export function AgendaSkeleton() {
  return (
    <div className="space-y-3" role="status" aria-label="Carregando agenda">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-12 w-full" />
      <Skeleton className="h-12 w-full" />
      <span className="sr-only">Carregando parcelas</span>
    </div>
  )
}
