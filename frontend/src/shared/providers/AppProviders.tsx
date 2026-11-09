import { useState, type PropsWithChildren } from 'react'
import {
  QueryClientProvider,
} from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { createQueryClient } from '@/shared/providers/query-client'
import { Toaster } from '@/shared/ui/toast'

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(createQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster />
      {import.meta.env.MODE === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  )
}
