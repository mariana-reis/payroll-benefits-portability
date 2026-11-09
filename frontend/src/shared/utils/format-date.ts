export function formatDate(value: string | Date, timeZone?: string): string {
  // Se for uma data pura no formato YYYY-MM-DD (ex.: dataDesconto de parcela), preserva a data civil
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [year, month, day] = value.split('-')
    return `${day}/${month}/${year}`
  }

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    throw new RangeError('A data informada é inválida.')
  }

  const formatter = new Intl.DateTimeFormat(
    'pt-BR',
    timeZone ? { timeZone } : undefined,
  )
  return formatter.format(date)
}

export function formatTime(value: string | Date, timeZone?: string): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    throw new RangeError('A data informada é inválida.')
  }

  const formatter = new Intl.DateTimeFormat('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    ...(timeZone ? { timeZone } : {}),
  })

  return formatter.format(date)
}
