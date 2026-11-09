import { AppLayout } from '@/shared/layouts/AppLayout'
import { PageContainer } from '@/shared/layouts/PageContainer'
import { PortabilidadePage } from '@/features/portabilidade/pages/PortabilidadePage'

function App() {
  return (
    <AppLayout>
      <PageContainer
        title="Portabilidade de Convênios"
        description="Autorize o beneficiário e consulte as parcelas disponíveis."
      />
      <PortabilidadePage />
    </AppLayout>
  )
}

export default App
