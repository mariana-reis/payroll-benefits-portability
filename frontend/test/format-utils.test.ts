import { describe, expect, it } from 'vitest'
import { formatCurrency } from '@/shared/utils/format-currency'
import { formatDate } from '@/shared/utils/format-date'

describe('format utilities', () => {
  it('formats currency in Brazilian Real', () => {
    expect(formatCurrency(1234.5)).toBe('R$ 1.234,50')
  })

  it('formats dates for Brazilian Portuguese', () => {
    expect(formatDate('2025-01-20T12:00:00.000Z')).toBe('20/01/2025')
  })
})
