import { Check } from 'lucide-react'
import { Badge } from '@/shared/ui/badge'
import { cn } from 'cn'

export interface StepItem {
  id: number
  title: string
}

// eslint-disable-next-line react-refresh/only-export-components
export const DEFAULT_STEPS: readonly StepItem[] = [
  { id: 1, title: 'Autorização' },
  { id: 2, title: 'Agenda' },
  { id: 3, title: 'Solicitação' },
  { id: 4, title: 'Acompanhamento' },
  { id: 5, title: 'Conclusão' },
] as const

export interface StepperProps {
  currentStep: number
  steps?: readonly StepItem[]
  className?: string
}

export function Stepper({
  currentStep,
  steps = DEFAULT_STEPS,
  className,
}: StepperProps) {
  return (
    <nav
      aria-label="Etapas da portabilidade"
      className={cn('w-full rounded-xl border bg-card p-4 shadow-sm', className)}
    >
      <ol className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-2">
        {steps.map((step, index) => {
          const isCompleted = step.id < currentStep
          const isCurrent = step.id === currentStep
          const isPending = step.id > currentStep

          return (
            <li
              key={step.id}
              aria-current={isCurrent ? 'step' : undefined}
              className={cn(
                'flex items-center gap-2.5 transition-colors',
                index < steps.length - 1 && 'sm:flex-1',
              )}
            >
              <div className="flex items-center gap-2">
                <span
                  data-testid={`step-indicator-${step.id}`}
                  className={cn(
                    'flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-all',
                    isCompleted && 'bg-primary text-primary-foreground',
                    isCurrent && 'bg-primary text-primary-foreground ring-4 ring-primary/20 font-bold',
                    isPending && 'bg-muted text-muted-foreground border',
                  )}
                >
                  {isCompleted ? (
                    <Check className="size-4" aria-hidden="true" />
                  ) : (
                    <span>{step.id}</span>
                  )}
                </span>
                <span
                  className={cn(
                    'text-xs sm:text-sm transition-colors',
                    isCurrent && 'font-bold text-foreground',
                    isCompleted && 'font-medium text-foreground',
                    isPending && 'text-muted-foreground',
                  )}
                >
                  {step.title}
                </span>
              </div>
              {isCompleted && (
                <Badge variant="outline" className="text-[10px] hidden sm:inline-flex py-0 px-1.5 h-4">
                  Concluído
                </Badge>
              )}
              {isCurrent && (
                <Badge variant="secondary" className="text-[10px] py-0 px-1.5 h-4">
                  Atual
                </Badge>
              )}
              {index < steps.length - 1 && (
                <div
                  aria-hidden="true"
                  className={cn(
                    'hidden sm:block h-px flex-1 transition-colors mx-2',
                    isCompleted ? 'bg-primary' : 'bg-border',
                  )}
                />
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}

