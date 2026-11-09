# Sistema de Portabilidade de Crédito Consignado - Frontend SPA

![React 19](https://img.shields.io/badge/React-19.0-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-6.0-646CFF?logo=vite&logoColor=white)
![Tailwind CSS v4](https://img.shields.io/badge/Tailwind%20CSS-v4.0-06B6D4?logo=tailwindcss&logoColor=white)
![Design System](https://img.shields.io/badge/UI-Base%20UI%20%2F%20shadcn-000000)
![State Management](https://img.shields.io/badge/State-TanStack%20Query%20v5-FF4154?logo=reactquery&logoColor=white)
![Tests](https://img.shields.io/badge/Tests-Vitest%20%7C%20Testing%20Library%20%7C%20MSW-FCC72B?logo=vitest&logoColor=black)
![ESLint](https://img.shields.io/badge/Linter-ESLint-4B32C3?logo=eslint&logoColor=white)
![Accessibility](https://img.shields.io/badge/A11y-WCAG%202.1%20AA-success)
![Mobile-First](https://img.shields.io/badge/Design-Mobile--First-blue)

Interface moderna (Single Page Application) desenvolvida em **React 19**, **TypeScript**, **Vite**, **Tailwind CSS v4** e componentes **shadcn/ui** (Base UI), destinada à gestão e acompanhamento do ciclo de vida de operações de portabilidade de crédito consignado entre instituições financeiras participantes da Central de Portabilidade.

---

## 📑 Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura e Padrões](#arquitetura-e-padrões)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Stack Tecnológica](#stack-tecnológica)
- [Fluxos da Aplicação](#fluxos-da-aplicação)
- [Polling Inteligente e Resiliência](#polling-inteligente-e-resiliência)
- [Linha do Tempo de Eventos](#linha-do-tempo-de-eventos)
- [Tratamento Refinado de Erros (RFC 7807)](#tratamento-refinado-de-erros-rfc-7807)
- [Acessibilidade (a11y) e UX](#acessibilidade-a11y-e-ux)
- [Responsividade](#responsividade)
- [Suíte de Testes](#suíte-de-testes)
- [Instalação e Execução](#instalação-e-execução)
- [Build e Empacotamento](#build-e-empacotamento)

---

## 🎯 Visão Geral

A aplicação frontend implementa o fluxo ponta a ponta de portabilidade de crédito consignado:
1. **Identificação e Validação de Opt-In**: Verificação se o beneficiário concedeu autorização formal de consulta na Central de Portabilidade. Em caso afirmativo, permite consulta imediata da agenda de contratos; caso contrário, provê fluxo guiado de autorização.
2. **Consulta da Agenda de Contratos**: Apresentação de parcelas, margem consignável, parcelas vincendas, taxa de juros atual e saldo devedor consolidado.
3. **Solicitação de Portabilidade com Idempotência**: Formulário de solicitação com geração segura de `Idempotency-Key` no padrão UUID v4 persistida na sessão, impedindo duplicidade de requisições financeiras.
4. **Acompanhamento Reativo (Status Tracker)**: Monitoramento em tempo real do processamento assíncrono via polling inteligente com respeito a cabeçalhos `Retry-After`.
5. **Timeline de Auditoria**: Visualização cronológica detalhada de todos os eventos da operação (`OPERACAO_CRIADA`, `PROTOCOLO_RECEBIDO`, `OPERACAO_CONFIRMADA`, `OPERACAO_REJEITADA`, etc.).

---

## 🏛 Arquitetura e Padrões

O projeto adota os princípios de **Clean Architecture** e **Screaming Architecture**, separando camadas técnicas e de domínio:

```
frontend/src/
├── app/                  # Configurações globais da aplicação (App, Providers, Router)
├── features/             # Módulos funcionais isolados (Vertical Slices)
│   └── portabilidade/    # Feature de Portabilidade de Crédito
│       ├── api/          # Clients de API, TanStack Query hooks e mutations
│       ├── components/   # Componentes visuais específicos da regra de negócio
│       ├── hooks/        # Custom hooks (polling, cálculo de stepper, lifecycle)
│       ├── pages/        # Telas e layouts principais da feature
│       └── types/        # Interfaces TypeScript e DTOs de resposta da API
└── shared/               # Recursos compartilhados transversais
    ├── ui/               # Design System primitives (Card, Badge, Button, Dialog...)
    └── utils/            # Utilitários puros de formatação, idempotência e tempo
```

### Princípios Chave:
- **Zero Business Logic in UI Primitives**: Componentes em `shared/ui` são agnósticos a regras financeiras.
- **Isolamento de Efeitos Colaterais**: Chamadas HTTP encapsuladas em hooks TanStack Query com cache ativado e invalidação seletiva.
- **Imutabilidade e Tipagem Estrita**: `strict: true` ativado em `tsconfig.json`.

---

## 🛠 Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|---|---|---|
| **Core Framework** | React 19 + TypeScript | Maior eficiência de renderização e segurança estática de tipos. |
| **Bundler & DevServer** | Vite 6 | Inicialização instantânea e compilação HMR ultrarrápida. |
| **Estilização** | Tailwind CSS v4 | CSS utility-first de alta performance com engine Oxide nativa. |
| **Componentes Base** | shadcn/ui & @base-ui/react | Acessibilidade nativa WAI-ARIA sem acoplamento a estilos proprietários. |
| **Gerenciamento de Estado de Servidor** | TanStack Query v5 | Cache automático, deduplicação de requests e gerenciamento reativo de polling. |
| **Ícones** | Lucide React | Conjunto consistente e acessível de ícones SVG. |
| **Mocks & Testes** | Vitest + Testing Library + MSW v2 | Execução paralela de testes de unidade e integração com interceptação real de rede. |

---

## 🔄 Fluxos da Aplicação

### 1. Stepper Interativo de 5 Etapas
O fluxo guia o usuário de forma clara através do componente `Stepper`:
- **Etapa 1 - Autorização**: Validação e consentimento de Opt-In.
- **Etapa 2 - Agenda**: Seleção e análise da dívida elegível.
- **Etapa 3 - Solicitação**: Configuração da proposta (instituição proponente e taxas).
- **Etapa 4 - Acompanhamento**: Processamento assíncrono com polling inteligente.
- **Etapa 5 - Conclusão**: Resolução final da portabilidade (Confirmada ou Rejeitada).

### 2. Acompanhamento e Notificação de Longa Duração
- **CopyProtocolButton**: Cópia de protocolo com 1 clique para a área de transferência com feedback visual e notificação acessível.
- **LongRunningNotice**: Ativado automaticamente caso uma operação permaneça em estado `PENDENTE` por mais de 60 segundos, instruindo o usuário com botão de atualização manual sem recarregar a tela.

---

## ⏱ Polling Inteligente e Resiliência

O monitoramento do status da operação é orquestrado pelo hook `useOperacaoPolling`:
- **Respeito ao cabeçalho `Retry-After`**: Se o backend retornar `retryAfterSeconds: 5`, o próximo polling aguarda exatamente 5.000 ms.
- **Fallback Exponencial Suave**: Caso o servidor não informe intervalo, adota-se intervalo padrão de 3 segundos.
- **Parada Automática**: O polling é interrompido imediatamente ao alcançar estados terminais (`CONFIRMADA`, `REJEITADA`).
- **Garantia de Não-Congelamento**: Utiliza hooks reativos com cleanup adequado no desmonte do componente.

---

## 📜 Linha do Tempo de Eventos

O componente `EventTimeline` apresenta o histórico de transições de estado:
- Ordenação estrita por `registradoEm` em ordem cronológica ascendente.
- Destaque semântico para cada tipo de evento:
  - `OPERACAO_CRIADA`: Criação do pedido no sistema.
  - `PROTOCOLO_RECEBIDO`: Recepção e validação do protocolo na Central.
  - `OPERACAO_CONFIRMADA`: Aceite e liquidação pelo banco credor original.
  - `OPERACAO_REJEITADA`: Recusa com exibição de motivo no `RejectionCard`.
  - `OPERACAO_FALHA_INTEGRACAO`: Alerta de falha na integração com botão de retentativa sob a mesma chave idempotente.
  - `OPERACAO_REPROCESSADA`: Reinício do ciclo de compensação.
- Exibição de carimbo de data/hora (UTC formatado para `pt-BR`), status anterior e status atual.

---

## 🛡 Tratamento Refinado de Erros (RFC 7807)

A aplicação consome e exibe payloads padronizados no formato `ProblemDetail`:

| Código HTTP | Cenário de Negócio | Comportamento no Frontend |
|---|---|---|
| **400 Bad Request** | Dados de entrada inválidos | Alerta inline amigável detalhando campos com inconsistência. |
| **404 Not Found** | Operação ou contrato não localizado | Alerta de busca sem resultados com botão para nova pesquisa. |
| **409 Conflict** | Opt-In ausente ou expirado | Alerta direcionado instruindo a realização do Opt-In prévio. |
| **422 Unprocessable** | Violação de regra de negócio funcional | Alerta explicativo com o motivo impeditivo da operação. |
| **503 Service Unavailable** | Central de Portabilidade indisponível | Banner de indisponibilidade com botão de retentativa automática. |

---

## ♿ Acessibilidade (a11y) e UX

- **Padrões WAI-ARIA**: Componentes críticos (`ErrorBanner`, `RejectionCard`, `StatusTracker`) implementam `role="alert"`, `aria-live="polite"` ou `aria-live="assertive"`.
- **Gerenciamento de Foco**: Foco automático programático (`focus management`) no `RejectionCard` ou na conclusão da operação para guiar leitores de tela.
- **Navegação por Teclado**: Suporte completo a navegação via `Tab`, `Space` e `Enter` em todos os modais, tabelas e botões.
- **Contraste e Legibilidade**: Paleta de cores em conformidade com as diretrizes WCAG 2.1 AA.

---

## 📱 Responsividade

A interface foi concebida sob a ótica **Mobile-First**, testada e validada nos seguintes breakpoints:
- **320px (Mobile Pequeno)**: Enquadramento vertical, tabelas roláveis horizontalmente com wrapper seguro, botões em largura total.
- **375px (Mobile Médio)**: Diagramação fluida de cartões com espaçamento ergonômico para toque (mínimo de 44x44px).
- **768px (Tablet)**: Exibição de métricas e formulários em duas colunas.
- **1024px+ (Desktop)**: Painéis com Stepper lateral e visualização expandida de timeline e contratos.

---

## 🧪 Suíte de Testes

Os testes cobrem tanto lógica isolada quanto fluxos ponta a ponta simulados via **Mock Service Worker (MSW)**:

```bash
# Executar todos os testes de unidade e integração
pnpm test

# Executar testes em modo watch interativo
pnpm test:watch

# Executar testes com relatório de cobertura
pnpm test:coverage
```

### Cenários Cobertos:
- ✅ Utilitários: formatação de moeda BRL, data/hora, geração de UUID de idempotência e cálculo de retry.
- ✅ Componentes visuais: `Stepper`, `CopyProtocolButton`, `EventTimeline`, `RejectionCard`, `ProblemDetailView`, `RetryBanner`.
- ✅ Fluxos completos com MSW:
  - Opt-In concedido e consulta da agenda de contratos.
  - Solicitação de portabilidade aprovada (`BEN001` - `CONFIRMADA`).
  - Solicitação de portabilidade rejeitada com motivo (`BEN002` - `REJEITADA`).
  - Falha transitória 503 com retentativa utilizando a mesma chave de idempotência (`BEN_ERRO`).
  - Disparo de aviso de longa duração (`LongRunningNotice`) após 60 segundos com Fake Timers.

---

## 🚀 Instalação e Execução

### Pré-requisitos:
- **Node.js**: Versão 20 LTS ou superior
- **pnpm**: Versão 9+ recomendada

### Passo a Passo:

```bash
# 1. Navegar até o diretório do frontend
cd frontend

# 2. Instalar dependências
pnpm install

# 3. Configurar variáveis de ambiente
cp .env.example .env
# No Windows (PowerShell): Copy-Item .env.example .env

# 4. Iniciar servidor de desenvolvimento (Vite)
pnpm dev
```

### Variáveis de Ambiente (`.env`)

A aplicação utiliza as seguintes variáveis de ambiente gerenciadas pelo Vite:

| Variável | Valor Padrão | Descrição |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | URL base do serviço backend Spring Boot para chamadas HTTP e polling. |

Caso o arquivo `.env` não seja criado, o cliente HTTP possui um fallback automático para `http://localhost:8080`, mas a criação explícita do arquivo a partir de `.env.example` é recomendada para consistência em todos os ambientes.

A aplicação estará acessível em: `http://localhost:5173`

---

## 📦 Build e Empacotamento

Para gerar os artefatos otimizados de produção:

```bash
# Compilar TypeScript e gerar bundle de produção via Vite
pnpm build

# Pré-visualizar o build localmente
pnpm preview
```

Os arquivos estáticos otimizados serão gerados no diretório `frontend/dist/`, prontos para servir em Nginx, Cloudflare Pages, S3/CloudFront ou qualquer CDN estática.
