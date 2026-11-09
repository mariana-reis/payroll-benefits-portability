import { describe, expect, it } from 'vitest'
import { formatCurrency } from '@/shared/utils/format-currency'
import { formatDate, formatTime } from '@/shared/utils/format-date'
import { generateIdempotencyKey } from '@/shared/utils/generate-idempotency-key'
import { retryInterval, isTerminalStatus } from '@/shared/utils/retry-interval'

describe('Unit Tests: Core Utilities', () => {
  describe('formatCurrency', () => {
    it('formats monetary values in Brazilian Real', () => {
      expect(formatCurrency(1234.5)).toBe('R$ 1.234,50')
      expect(formatCurrency(0)).toBe('R$ 0,00')
      expect(formatCurrency(150)).toBe('R$ 150,00')
    })
  })

  describe('formatDate and formatTime', () => {
    it('formats dates in pt-BR with local or explicit timezone', () => {
      expect(formatDate('2026-11-08')).toBe('08/11/2026')
      expect(formatDate('2026-11-08T20:35:00Z', 'UTC')).toBe('08/11/2026')
      expect(formatDate('2026-11-08T20:35:00Z', 'America/Sao_Paulo')).toBe('08/11/2026')
    })

    it('formats times converting from UTC to local America/Sao_Paulo timezone', () => {
      expect(formatTime('2026-11-09T03:44:03Z', 'America/Sao_Paulo')).toBe('00:44:03')
      expect(formatTime('2026-11-08T20:35:10Z', 'UTC')).toBe('20:35:10')
    })

    it('throws on invalid dates', () => {
      expect(() => formatDate('not-a-date')).toThrow(RangeError)
      expect(() => formatTime('not-a-date')).toThrow(RangeError)
    })
  })

  describe('generateIdempotencyKey', () => {
    it('generates a valid UUID string', () => {
      const key1 = generateIdempotencyKey()
      const key2 = generateIdempotencyKey()

      expect(typeof key1).toBe('string')
      expect(key1).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
      )
      expect(key1).not.toBe(key2)
    })
  })

  describe('retryInterval', () => {
    it('identifies terminal status', () => {
      expect(isTerminalStatus('CONFIRMADA')).toBe(true)
      expect(isTerminalStatus('REJEITADA')).toBe(true)
      expect(isTerminalStatus('PENDENTE')).toBe(false)
      expect(isTerminalStatus('FALHA_INTEGRACAO')).toBe(false)
      expect(isTerminalStatus(null)).toBe(false)
      expect(isTerminalStatus(undefined)).toBe(false)
    })

    it('returns false for terminal statuses', () => {
      expect(retryInterval('CONFIRMADA', 2)).toBe(false)
      expect(retryInterval('REJEITADA', 5)).toBe(false)
    })

    it('returns retryAfterSeconds in milliseconds when positive', () => {
      expect(retryInterval('PENDENTE', 5)).toBe(5000)
      expect(retryInterval('PENDENTE', 10)).toBe(10000)
    })

    it('returns default 2000ms when retryAfterSeconds is absent or <= 0', () => {
      expect(retryInterval('PENDENTE', null)).toBe(2000)
      expect(retryInterval('PENDENTE', undefined)).toBe(2000)
      expect(retryInterval('PENDENTE', 0)).toBe(2000)
      expect(retryInterval('PENDENTE', -1)).toBe(2000)
      expect(retryInterval('FALHA_INTEGRACAO', null)).toBe(2000)
    })
  })
})

