import { useQuery } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AppProviders } from '@/shared/providers/AppProviders'

function QueryProbe() {
  const { data } = useQuery({
    queryKey: ['provider-probe'],
    queryFn: async () => 'Query Provider ativo',
  })

  return <p>{data ?? 'Carregando'}</p>
}

describe('Query provider', () => {
  it('provides a working TanStack Query client', async () => {
    render(<QueryProbe />, { wrapper: AppProviders })

    expect(await screen.findByText('Query Provider ativo')).toBeInTheDocument()
  })
})
