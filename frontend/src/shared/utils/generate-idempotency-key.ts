export function generateIdempotencyKey(): string {
  if (!globalThis.crypto?.randomUUID) {
    throw new Error('Este ambiente não oferece suporte a crypto.randomUUID().')
  }

  return globalThis.crypto.randomUUID()
}
