import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CopyProtocolButton } from '@/features/portabilidade/components/CopyProtocolButton'
import { EventTimeline } from '@/features/portabilidade/components/EventTimeline'
import { ProblemDetailView } from '@/features/portabilidade/components/ProblemDetailView'
import { RetryBanner } from '@/features/portabilidade/components/RetryBanner'
import { Stepper, DEFAULT_STEPS } from '@/features/portabilidade/components/Stepper'
import type { EventoResponse } from '@/features/portabilidade/types/portabilidade'
import { formatDate, formatTime } from '@/shared/utils/format-date'
import { renderWithProviders } from './test-utils'

describe('Component Tests: Stepper', () => {
  it('renders all 5 steps and highlights the current step', () => {
    renderWithProviders(<Stepper currentStep={2} />)

    DEFAULT_STEPS.forEach((step) => {
      expect(screen.getByText(step.title)).toBeInTheDocument()
    })

    const step2 = screen.getByRole('listitem', { current: 'step' })
    expect(step2).toHaveTextContent('Agenda')
    expect(step2).toHaveTextContent('Atual')

    const step1 = screen.getByText('Autorização').closest('li')
    expect(step1).toHaveTextContent('Concluído')

    const step3 = screen.getByText('Solicitação').closest('li')
    expect(step3).not.toHaveTextContent('Concluído')
    expect(step3).not.toHaveTextContent('Atual')
  })
})

describe('Component Tests: CopyProtocolButton', () => {
  it('copies the protocol to clipboard and displays feedback', async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    })

    renderWithProviders(<CopyProtocolButton protocolo="prot-12345" />)

    const button = screen.getByRole('button', { name: 'Copiar protocolo' })
    expect(button).toBeInTheDocument()

    fireEvent.click(button)

    expect(writeTextMock).toHaveBeenCalledWith('prot-12345')
    expect(await screen.findByText('Copiado')).toBeInTheDocument()
  })
})

describe('Component Tests: EventTimeline', () => {
  const mockEventos: EventoResponse[] = [
    {
      id: 'evt-2',
      tipoEvento: 'OPERACAO_CONFIRMADA',
      statusAnterior: 'PENDENTE',
      statusNovo: 'CONFIRMADA',
      detalhe: 'Operação confirmada pela Central de Convênios.',
      registradoEm: '2026-11-08T20:36:00Z',
    },
    {
      id: 'evt-1',
      tipoEvento: 'OPERACAO_CRIADA',
      statusAnterior: null,
      statusNovo: 'PENDENTE',
      detalhe: 'Operação de portabilidade registrada localmente.',
      registradoEm: '2026-11-08T20:35:00Z',
    },
    {
      id: 'evt-3',
      tipoEvento: 'PROTOCOLO_RECEBIDO',
      statusAnterior: 'PENDENTE',
      statusNovo: 'PENDENTE',
      detalhe: 'Protocolo atribuído pela Central.',
      registradoEm: '2026-11-08T20:35:30Z',
    },
  ]

  it('renders events sorted in ascending chronological order', () => {
    renderWithProviders(<EventTimeline eventos={mockEventos} />)

    const items = screen.getAllByTestId('timeline-item')
    expect(items).toHaveLength(3)

    // First item should be OPERACAO_CRIADA (earliest timestamp)
    const evCriada = mockEventos.find((e) => e.tipoEvento === 'OPERACAO_CRIADA')!
    expect(items[0]).toHaveTextContent('OPERACAO_CRIADA')
    expect(items[0]).toHaveTextContent('Operação de portabilidade registrada localmente.')
    expect(items[0]).toHaveTextContent(formatDate(evCriada.registradoEm))
    expect(items[0]).toHaveTextContent(formatTime(evCriada.registradoEm))

    // Second item should be PROTOCOLO_RECEBIDO (20:35:30)
    expect(items[1]).toHaveTextContent('PROTOCOLO_RECEBIDO')

    // Third item should be OPERACAO_CONFIRMADA (20:36:00)
    expect(items[2]).toHaveTextContent('OPERACAO_CONFIRMADA')
    expect(items[2]).toHaveTextContent('PENDENTE')
    expect(items[2]).toHaveTextContent('CONFIRMADA')
  })

  it('renders an empty state message when no events exist', () => {
    renderWithProviders(<EventTimeline eventos={[]} />)

    expect(screen.getByText('Nenhum evento registrado até o momento.')).toBeInTheDocument()
  })
})

describe('Component Tests: ProblemDetailView and RetryBanner', () => {
  it('renders RFC 7807 problem details', () => {
    renderWithProviders(
      <ProblemDetailView
        problem={{
          title: 'Central indisponível',
          status: 503,
          detail: 'Falha temporária de comunicação com a Central.',
          correlationId: 'corr-xyz-123',
        }}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Central indisponível')
    expect(screen.getByRole('alert')).toHaveTextContent('HTTP 503')
    expect(screen.getByRole('alert')).toHaveTextContent('Falha temporária de comunicação com a Central.')
    expect(screen.getByRole('alert')).toHaveTextContent('corr-xyz-123')
  })

  it('renders RetryBanner and triggers callback on click', () => {
    const onRetry = vi.fn()
    renderWithProviders(<RetryBanner onRetry={onRetry} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Falha de integração')
    const button = screen.getByRole('button', { name: 'Tentar novamente' })
    fireEvent.click(button)
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

