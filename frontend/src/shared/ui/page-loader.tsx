import { Skeleton } from '@/shared/ui/skeleton'

export function PageLoader() {
  return (
    <div className="space-y-4" role="status" aria-label="Carregando conteúdo">
      <Skeleton className="h-8 w-56" />
      <Skeleton className="h-32 w-full" />
      <span className="sr-only">Carregando conteúdo</span>
    </div>
  )
}
