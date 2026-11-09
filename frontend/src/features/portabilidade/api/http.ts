export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  [extension: string]: unknown
}

export type RequestConfig = Omit<RequestInit, 'body'> & {
  body?: unknown
}

export class ApiError extends Error {
  readonly status: number
  readonly problemDetail: ProblemDetail

  constructor(status: number, problemDetail: ProblemDetail, options?: ErrorOptions) {
    super(
      problemDetail.detail ??
        problemDetail.title ??
        `A solicitação falhou com status ${status}.`,
      options,
    )
    this.name = 'ApiError'
    this.status = status
    this.problemDetail = problemDetail
  }
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    return false
  }

  const problem = value as Record<string, unknown>
  return (
    (problem.type === undefined || typeof problem.type === 'string') &&
    (problem.title === undefined || typeof problem.title === 'string') &&
    (problem.status === undefined || typeof problem.status === 'number') &&
    (problem.detail === undefined || typeof problem.detail === 'string') &&
    (problem.instance === undefined || typeof problem.instance === 'string')
  )
}

async function parseProblemDetail(response: Response): Promise<ProblemDetail> {
  const contentType = response.headers.get('content-type')?.split(';')[0].trim().toLowerCase()

  if (contentType !== 'application/problem+json') {
    const detail = await response.text()
    return {
      type: 'about:blank',
      title: response.statusText || 'Falha na solicitação',
      status: response.status,
      ...(detail ? { detail } : {}),
    }
  }

  const body = await response.text()
  let parsed: unknown

  try {
    parsed = JSON.parse(body)
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new ApiError(
        response.status,
        {
          type: 'about:blank',
          title: 'Resposta Problem Details inválida',
          status: response.status,
          detail: 'A API retornou um documento JSON malformado.',
        },
        { cause: error },
      )
    }
    throw error
  }

  if (!isProblemDetail(parsed)) {
    throw new ApiError(response.status, {
      type: 'about:blank',
      title: 'Resposta Problem Details inválida',
      status: response.status,
      detail: 'A API retornou um documento Problem Details fora do formato esperado.',
    })
  }

  return parsed
}

function getApiBaseUrl(): string {
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
  return apiUrl.endsWith('/') ? apiUrl.slice(0, -1) : apiUrl
}

export async function apiRequest<TResponse>(
  path: string,
  config: RequestConfig = {},
): Promise<TResponse> {
  const { body, headers: requestHeaders, ...requestOptions } = config
  const headers = new Headers(requestHeaders)

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(`${getApiBaseUrl()}${path.startsWith('/') ? path : `/${path}`}`, {
      ...requestOptions,
      headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    })
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError(
      503,
      {
        type: 'about:blank',
        title: 'Central indisponível',
        status: 503,
        detail:
          'Não foi possível conectar ao servidor. Verifique sua conexão e se a API está em execução.',
      },
      { cause: error },
    )
  }

  if (!response.ok) {
    throw new ApiError(response.status, await parseProblemDetail(response))
  }

  if (response.status === 204) {
    return undefined as TResponse
  }

  return (await response.json()) as TResponse
}
