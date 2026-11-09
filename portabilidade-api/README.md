# Microsserviço de Portabilidade de Convênios (`portabilidade-api`)

![Java 21](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data-JPA-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Migrations-Flyway-CC0202?logo=flyway&logoColor=white)
![Security HMAC](https://img.shields.io/badge/Security-HMAC--SHA256-blueviolet)
![Error Handling](https://img.shields.io/badge/RFC-7807%20Problem%20Detail-blue)
![Documentation](https://img.shields.io/badge/OpenAPI-3.0%20Swagger-85EA2D?logo=swagger&logoColor=black)
![Observability](https://img.shields.io/badge/Observability-Spring%20Actuator-brightgreen)
![Tests](https://img.shields.io/badge/Tests-JUnit%205%20%7C%20MockMvc-25A162)

Módulo backend responsável pelo processamento transacional de portabilidade de crédito consignado, controle de consentimento (opt-in), comunicação resiliente com a Central de Convênios e auditoria imutável por eventos.

---

## Visão Geral

O serviço expõe uma API RESTful corporativa que abstrai e orquestra as regras do processo de portabilidade. Toda a operação é orientada a consistência transacional: nenhum registro de operação é perdido mesmo sob falhas de rede ou indisponibilidade temporária de parceiros externos.

---

## Estrutura do Módulo

O código-fonte segue a organização por domínio (`features`), garantindo baixo acoplamento e alta coesão:

```
com.portabilidade.api/
├── agenda/              # Consulta e validação de contratos e parcelas consignadas
│   ├── controller/      # AgendaController (GET /agenda/{beneficiarioId})
│   ├── dto/             # AgendaResponse, ParcelaResponse
│   ├── exception/       # AutorizacaoObrigatoriaException
│   └── service/         # AgendaService
├── autorizacao/         # Gestão do consentimento prévio do beneficiário (Opt-In)
│   ├── controller/      # OptInController (POST /opt-in/{beneficiarioId})
│   ├── entity/          # Autorizacao (persistida com identificador gerado pela Central)
│   ├── repository/      # AutorizacaoRepository
│   └── service/         # OptInService
├── central/             # Camada de cliente HTTP e contratos da Central de Convênios
│   ├── client/          # CentralClient com RestClient e interceptadores
│   ├── exception/       # CentralIndisponivelException, ProtocoloNaoEncontradoException
│   ├── properties/      # CentralProperties, CentralRetryProperties
│   └── security/        # HmacSigner (assinatura digital HMAC-SHA256)
├── config/              # Infraestrutura, segurança, documentação e observabilidade
│   ├── exception/       # GlobalExceptionHandler (RFC 7807 ProblemDetail)
│   ├── observability/   # CorrelationIdFilter (MDC & rastreamento)
│   ├── openapi/         # OpenApiConfiguration (SpringDoc OpenAPI / Swagger)
│   └── web/             # CorsConfig
└── operacao/            # Domínio principal da operação de portabilidade
    ├── controller/      # OperacaoController, SolicitacaoController
    ├── domain/          # MaquinaEstadoOperacao (regras estritas de transição)
    ├── dto/             # OperacaoResponse, EventoResponse, SolicitarPortabilidadeRequest
    ├── entity/          # OperacaoPortabilidade, OperacaoParcela, OperacaoEventoHistorico
    ├── event/           # OperacaoEventoService (registro imutável de eventos)
    ├── hash/            # RequestHashService (normalização e hash SHA-256)
    ├── idempotency/     # IdempotencyService
    └── service/         # SolicitacaoService, StatusOperacaoService, OperacaoPersistenciaService
```

---

## Banco de Dados & Flyway

- **Banco Padrão:** H2 Database em memória (com opção de arquivo em disco para desenvolvimento contínuo).
- **Gerenciamento de Schema:** Migrations versionadas controladas pelo **Flyway**.
- **Localização dos scripts:** `src/main/resources/db/migration/V1__init.sql`.
- **Estratégia:** O Hibernate atua em modo de validação (`ddl-auto: validate`), garantindo que o banco de dados seja governado estritamente pelos scripts de migração versionados.

---

## Documentação Interativa (Swagger / OpenAPI)

A API disponibiliza a especificação OpenAPI 3.0 via SpringDoc:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Schema (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Todos os DTOs e endpoints contam com descrições e payloads de exemplo para facilidade de integração.

---

## Observabilidade & Actuator

- **Spring Boot Actuator:**
  - Health check: `GET /actuator/health`
  - Informações de build/ambiente: `GET /actuator/info`
- **Rastreabilidade Fim a Fim (CorrelationId):**
  - Implementado via `CorrelationIdFilter`.
  - Captura ou gera o identificador através do cabeçalho `X-Correlation-Id`.
  - Injeta o valor no `MDC` (Mapped Diagnostic Context) do SLF4J para formatação padronizada em todos os logs.
  - Propaga automaticamente o identificador para as chamadas externas efetuadas pelo `CentralClient`.
  - Retorna o `correlationId` em qualquer payload de erro padronizado RFC 7807.

---

## Endpoints da API

Base: `/api/v1/portabilidade`

| Método | Endpoint | Descrição | Cabeçalhos Específicos |
|---|---|---|---|
| `POST` | `/opt-in/{beneficiarioId}` | Registra o consentimento formal do beneficiário. | — |
| `GET` | `/agenda/{beneficiarioId}` | Consulta parcelas consignadas disponíveis. | — |
| `POST` | `/solicitar` | Submete a solicitação de portabilidade. | `X-Idempotency-Key` (obrigatório, UUID) |
| `GET` | `/operacao/{id}` | Consulta o status da operação e a trilha cronológica de eventos. | — |

---

## Idempotência & Prevenção de Duplicidade

A API implementa idempotência robusta baseada em dois fatores:
1. **Chave Exclusiva:** O cliente fornece um identificador UUID no cabeçalho `X-Idempotency-Key`.
2. **Hash do Payload (SHA-256):** O corpo da requisição é normalizado e tem seu resumo criptográfico calculado (`RequestHashService`).

### Comportamento Transacional:
- **Mesma Chave + Mesmo Hash:** Retorna a operação já existente (código HTTP 200/202) sem gerar novos efeitos colaterais.
- **Mesma Chave + Hash Diferente:** Rejeita imediatamente com erro funcional HTTP 422 (`IdempotencyPayloadMismatchException`).
- **Retentativa após Falha:** Caso uma operação anterior tenha resultado em `FALHA_INTEGRACAO`, a mesma chave pode ser reenviada com segurança, reutilizando a operação persistida e tentando novamente a integração.

---

## Trilha de Eventos Imutável

Cada operação mantém uma tabela associada de histórico (`operacao_evento_historico`), gerando registros imutáveis a cada transição:

1. `OPERACAO_CRIADA`: Instância inicial criada e persistida localmente.
2. `PROTOCOLO_RECEBIDO`: Protocolo aceito e atribuído pela Central.
3. `OPERACAO_CONFIRMADA`: Conclusão positiva da liquidação do convênio.
4. `OPERACAO_REJEITADA`: Recusa informada pela Central, persistindo o `motivoRejeicao`.
5. `OPERACAO_FALHA_INTEGRACAO`: Registro de falha ou timeout na comunicação remota.
6. `OPERACAO_REPROCESSADA`: Reenvio ou retentativa explícita de integração.

A consulta `GET /operacao/{id}` retorna todos os eventos ordenados cronologicamente por `registradoEm`.

---

## Integridade Digital (HMAC-SHA256)

Para proteger a integridade de requisições de alteração de estado submetidas à Central:
- O componente `HmacSigner` calcula a assinatura criptográfica:
  $$\text{assinatura} = \text{HMAC-SHA256}(\text{chaveSecreta}, \text{timestamp} + "." + \text{payloadJSON})$$
- Os dados são enviados à Central com os cabeçalhos:
  - `X-Timestamp`: Época Unix em segundos da emissão.
  - `X-Signature`: Digest hexadecimal da assinatura gerada.

---

## Resiliência & Máquina de Estados

### Resiliência HTTP
- **Timeouts Rígidos:** Conexão configurada para 3 segundos; leitura para 5 segundos.
- **Retry Seletivo:** Retentativas automáticas são autorizadas **apenas para métodos idempotentes (GET)**. Operações de mutação (`POST`) jamais realizam retry cego, protegendo contra duplicidade externa.
- **Consulta sob Demanda:** O backend consulta o status na Central quando solicitado pela interface, eliminando jobs agendados desnecessários em background.

### Máquina de Estados da Operação
```
              ┌───────────────┐
              │   PENDENTE    │
              └──┬────┬─────┬─┘
                 │    │     │
    ┌────────────┘    │     └────────────┐
    ▼                 ▼                  ▼
┌───────────┐  ┌───────────┐   ┌───────────────────┐
│CONFIRMADA │  │ REJEITADA │   │ FALHA_INTEGRACAO  │
└───────────┘  └───────────┘   └─────────┬─────────┘
                                         │
                                         ▼ (Retentativa sob mesma chave)
                               ┌───────────────────┐
                               │   PENDENTE        │
                               └───────────────────┘
```
Estados terminais (`CONFIRMADA` e `REJEITADA`) são definitivos e rejeitam qualquer transição subsequente.

---

## Testes Automatizados

A suíte de testes cobre integralmente os cenários de sucesso, erro, resiliência e integridade:

- **Testes Unitários:** Validação de regras de negócio, cálculo de hash, assinatura HMAC e máquina de estados.
- **Testes de Integração com Banco:** `ModelagemRepositoryTests` validando constraints e Flyway contra o banco real.
- **Contratos e Resiliência (WireMock):** `CentralClientTests`, `SolicitacaoControllerIntegrationTests`, `OperacaoControllerIntegrationTests` simulando timeouts, falhas 503, concorrência e idempotência.

Para executar todos os testes da API:
```bash
mvn test
```
*Em Windows:*
```powershell
.\mvnw.cmd test
```

---

## Como Executar

### 1. Definir Variáveis de Ambiente (Opcional)
```bash
export CENTRAL_API_URL=http://localhost:4000
export CENTRAL_HMAC_SECRET=segredo-padrao-central
```

### 2. Iniciar a Aplicação
```bash
mvn spring-boot:run
```
*Em Windows:*
```powershell
.\mvnw.cmd spring-boot:run
```
*O servidor subirá em `http://localhost:8080`.*

