import { useState } from 'react'
import { Check, Copy } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { toast } from '@/shared/ui/toast'

export interface CopyProtocolButtonProps {
  protocolo: string
  className?: string
}

export function CopyProtocolButton({ protocolo, className }: CopyProtocolButtonProps) {
  const [hasCopied, setHasCopied] = useState(false)

  async function handleCopy() {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(protocolo)
      }
    } catch {
      // Fallback if clipboard API is not permitted
    }

    setHasCopied(true)
    try {
      toast.create?.({
        title: 'Protocolo copiado',
        type: 'success',
      })
    } catch {
      // safe fallback
    }

    setTimeout(() => {
      setHasCopied(false)
    }, 2000)
  }

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      className={className}
      onClick={handleCopy}
      aria-label="Copiar protocolo"
    >
      {hasCopied ? (
        <>
          <Check className="size-4 text-emerald-600" aria-hidden="true" />
          <span>Copiado</span>
        </>
      ) : (
        <>
          <Copy className="size-4" aria-hidden="true" />
          <span>Copiar protocolo</span>
        </>
      )}
    </Button>
  )
}
