const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export function formatCurrency(value: number): string {
  if (!Number.isFinite(value)) {
    throw new RangeError('O valor monetário deve ser um número finito.')
  }

  return currencyFormatter.format(value)
}
