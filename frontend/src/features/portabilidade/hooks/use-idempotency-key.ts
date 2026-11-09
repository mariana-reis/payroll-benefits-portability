import { useCallback, useState } from 'react'
import { generateIdempotencyKey } from '@/shared/utils/generate-idempotency-key'

export function useIdempotencyKey() {
  const [key, setKey] = useState(generateIdempotencyKey)
  const rotate = useCallback(() => {
    setKey(generateIdempotencyKey())
  }, [])

  return { key, rotate }
}
