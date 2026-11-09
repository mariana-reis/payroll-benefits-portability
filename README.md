# Plataforma de Portabilidade de Convênios

![Java 21](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.4+-6DB33F?logo=springboot&logoColor=white)
![React 19](https://img.shields.io/badge/React-19.0-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4.0-06B6D4?logo=tailwindcss&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-6.0-646CFF?logo=vite&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0202?logo=flyway&logoColor=white)
![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0-85EA2D?logo=swagger&logoColor=black)
![RFC 7807](https://img.shields.io/badge/RFC-7807%20Problem%20Details-blue)
![WCAG 2.1 AA](https://img.shields.io/badge/A11y-WCAG%202.1%20AA-success)
![License](https://img.shields.io/badge/License-MIT-green)

Sistema corporativo para gestão e liquidação de portabilidade de crédito e convênios com desconto em folha, atuando na intermediação entre beneficiários, instituição credora proponente e a Central de Convênios.

---

## Visão Geral

A **Plataforma de Portabilidade de Convênios** viabiliza o processo operacional pelo qual uma instituição financeira assume os direitos creditórios de parcelas consignadas futuras.

A jornada do sistema é estruturada em 5 etapas fundamentais:
1. **Autorização (Opt-In):** Registro de consentimento formal do beneficiário perante a Central de Convênios.
2. **Consulta de Agenda:** Recuperação das parcelas ativas, contratos e saldos disponíveis para portabilidade.
3. **Solicitação de Portabilidade:** Seleção das parcelas elegíveis, indicação de conta de liquidação e submissão com garantia estrita de idempotência.
4. **Acompanhamento Operacional:** Polling adaptativo com respeito ao cabeçalho `Retry-After`, gestão de concorrência e auditoria de estados.
5. **Conclusão:** Apresentação clara do desfecho (confirmação com protocolo ou detalhamento de motivos de recusa pela Central) e trilha histórica completa de eventos.

---

## Arquitetura

O ecossistema é baseado em uma arquitetura desacoplada e distribuída, dividida em três pilares:

```
┌─────────────────────────────────┐
│       Frontend SPA (React)      │
│  - Tailwind CSS + shadcn/ui     │
│  - TanStack Query (Polling)     │
│  - Stepper + EventTimeline      │
└───────────────┬─────────────────┘
                │ HTTP REST / JSON
                │ RFC 7807 (Problem Details)
                ▼
┌─────────────────────────────────┐
│     Backend API (Spring Boot)   │
│  - Máquina de Estados Finita    │
│  - Idempotência SHA-256 + UUID  │
│  - Trilha de Eventos Imutável   │
│  - Assinatura Digital HMAC      │
│  - Resiliência & Observabilidade│
└───────────────┬─────────────────┘
                │ REST Client + HMAC-SHA256
                │ Retry apenas em consultas idempotentes
                ▼
┌─────────────────────────────────┐
│      Central de Convênios       │
│      (Serviço Autorizador)      │
└─────────────────────────────────┘
```

### Princípios Arquiteturais Aplicados
- **Persistir antes de integrar:** Toda solicitação de negócio é persistida localmente em estado preliminar antes de qualquer comunicação remota com a Central, eliminando perda de contexto operacional em caso de instabilidades.
- **Idempotência por Chave e Hash:** Prevenção contra duplo clique e reenvios através da combinação de chave de idempotência (`X-Idempotency-Key`) e hash criptográfico (SHA-256) do payload canônico.
- **Assinatura Digital HMAC-SHA256:** Proteção de integridade nos pacotes de envio crítico com inclusão de `X-Signature` e `X-Timestamp`.
- **Contratos Padronizados:** Respostas de erro aderentes ao padrão RFC 7807 (`application/problem+json`).
- **Observabilidade Fim a Fim:** Propagação de `X-Correlation-Id` em todos os saltos de rede, contextualizado no MDC para rastreamento nos logs corporativos.

---

## Estrutura do Repositório

```
project-portabilidade-de-convenios/
├── portabilidade-api/         # Microsserviço Backend (Java 21, Spring Boot 4.x)
│   ├── src/main/java/         # Código de domínio, regras, integrações e segurança
│   ├── src/main/resources/    # Configurações de aplicação e migrações Flyway
│   └── src/test/java/         # Testes unitários, integração e contratos WireMock
├── frontend/                  # Single Page Application (React 19, TypeScript, Vite)
│   ├── src/features/          # Módulos verticais de negócio (portabilidade)
│   ├── src/shared/            # Componentes reutilizáveis, layouts e utilitários
│   └── test/                  # Testes de integração (MSW), componentes e unitários
└── simulador/                 # Simulador local da Central de Convênios
    └── mock-central.js        # Servidor mock da autorizadora externa (porta 4000)
```

---

## Tecnologias

### Backend
- **Linguagem & Framework:** Java 21 LTS, Spring Boot 4.1.2
- **Persistência & Migração:** Spring Data JPA, Hibernate, Flyway, H2 Database (em memória/arquivo)
- **Segurança & Criptografia:** HMAC-SHA256 nativo, hash SHA-256
- **Resiliência & Conectividade:** Spring WebMvc, RestClient com timeouts estritos e retry seletivo
- **Documentação & Métricas:** SpringDoc OpenAPI 3.2.0 (Swagger UI), Spring Boot Actuator

### Frontend
- **Interface & Linguagem:** React 19, TypeScript 5.8+, Vite 8
- **Estilização & Componentes:** Tailwind CSS v4, Base UI, shadcn/ui, Lucide React
- **Gerenciamento de Estado de Rede:** TanStack Query v5
- **Formulários & Validação:** React Hook Form, Zod
- **Testes & Qualidade:** Vitest, Testing Library, MSW (Mock Service Worker), ESLint

---

## Como Executar

### Pré-requisitos
- **Git:** Instalado e configurado no ambiente
- **Java Development Kit (JDK):** Versão 21 ou superior
- **Node.js & pnpm:** Node.js v20+ e pnpm instalado globalmente
- **Portas locais livres:** `4000` (Central Mock), `8080` (Backend API), `5173` (Frontend SPA)

### 1. Clonar o Repositório
```bash
git clone https://github.com/mariana-reis/payroll-benefits-portability.git
cd payroll-benefits-portability
```

### 2. Iniciar o Simulador da Central
Em um terminal:
```bash
cd simulador
node mock-central.js
```
*O serviço iniciará na porta `4000`.*

### 3. Iniciar o Backend API
Em um novo terminal:
```bash
cd portabilidade-api
./mvnw spring-boot:run
```
*Em Windows:*
```powershell
cd portabilidade-api
.\mvnw.cmd spring-boot:run
```
*A API estará pronta em `http://localhost:8080`.*
- Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`
- Health check (Actuator): `http://localhost:8080/actuator/health`

### 4. Iniciar o Frontend SPA
Em um terceiro terminal:
```bash
cd frontend
pnpm install
cp .env.example .env
pnpm dev
```
*Em ambientes Windows (PowerShell):*
```powershell
cd frontend
pnpm install
Copy-Item .env.example .env
pnpm dev
```
> **Nota de Configuração (`.env`):** O arquivo `.env` gerado a partir do `.env.example` define a variável `VITE_API_URL=http://localhost:8080`, indicando o endereço da API Backend para as requisições HTTP e WebSockets/Polling da aplicação.

*Acesse a aplicação no navegador em `http://localhost:5173`.*

---

## Cenários Operacionais de Validação

O simulador e a plataforma possuem três perfis padrão de beneficiários para homologação de fluxos:

| Identificador | Perfil / Comportamento Esperado | Desfecho na Plataforma |
|---|---|---|
| **BEN001** | Margem disponível com duas parcelas elegíveis na agenda. | Fluxo completo autorizado, submetido, processado via polling e **CONFIRMADA**. |
| **BEN002** | Beneficiário com margem comprometida perante as regras centrais. | Solicitação aceita e encaminhada, resultando em recusa da Central com status **REJEITADA** e justificativa consignável. |
| **BEN_ERRO** | Simulação de indisponibilidade momentânea da Central externa (HTTP 503). | Transição segura para **FALHA_INTEGRACAO** com persistência dos dados e permissão de retentativa segura sob a mesma chave de idempotência. |

---

## Roadmap

- [x] Motor de idempotência estrita via chave única e payload digest.
- [x] Assinatura digital HMAC-SHA256 para comunicação com a Central.
- [x] Trilha de auditoria cronológica e imutável de eventos operacionais.
- [x] Polling adaptativo orientado ao cabeçalho `Retry-After`.
- [x] Interface rica acessível (WCAG) com Stepper de progresso e visualização de eventos.
- [ ] Suporte a webhooks assíncronos reversos da Central para notificação push de liquidação.
- [ ] Exportação de comprovantes de portabilidade em formato PDF assinado digitalmente.
- [ ] Clusterização de sessão e cache distribuído com Redis em ambientes multi-região.

---