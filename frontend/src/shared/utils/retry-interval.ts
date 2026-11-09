import type { StatusOperacao } from '@/features/portabilidade/types/portabilidade'

export function isTerminalStatus(
  status: StatusOperacao | string | null | undefined,
): boolean {
  return status === 'CONFIRMADA' || status === 'REJEITADA'
}

export function retryInterval(
  status: StatusOperacao | string | null | undefined,
  retryAfterSeconds: number | null | undefined,
  defaultIntervalSeconds = 2,
): number | false {
  if (isTerminalStatus(status)) {
    return false
  }

  const seconds =
    typeof retryAfterSeconds === 'number' && retryAfterSeconds > 0
      ? retryAfterSeconds
      : defaultIntervalSeconds

  return seconds * 1000
}

