import * as React from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'

export interface RejectionCardProps {
  motivoRejeicao: string | null
  contentRef?: React.Ref<HTMLParagraphElement>
}

export function RejectionCard({ motivoRejeicao, contentRef }: RejectionCardProps) {
  return (
    <Card className="border-destructive/50 bg-destructive/5">
      <CardHeader>
        <CardTitle className="text-destructive font-semibold">Portabilidade rejeitada</CardTitle>
        <CardDescription>Confira o motivo informado pela Central para esta operação.</CardDescription>
      </CardHeader>
      <CardContent>
        <p
          ref={contentRef}
          tabIndex={-1}
          role="alert"
          aria-live="assertive"
          className="text-sm font-medium text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-md p-1"
        >
          {motivoRejeicao || 'A operação foi rejeitada sem um motivo detalhado.'}
        </p>
      </CardContent>
    </Card>
  )
}
