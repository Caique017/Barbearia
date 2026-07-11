# CONTEXT.md — Barbearia API

## Objetivo desse arquivo
Esse arquivo serve como fonte de verdade do projeto para o Claude Code.
Ao revisar o código, valide se as implementações batem com as regras definidas aqui.

---

## Sobre o projeto

Sistema web de agendamento para barbearia. MVP com dois perfis: **Cliente** (agenda cortes, vê histórico) e **Barbeiro** (gerencia agenda, clientes e financeiro). Projeto pensado para escalar para SaaS multi-tenant no futuro.

---

## Stack

- **Java 21**
- **Spring Boot 4.1**
- **Spring Security + JWT** (autenticação)
- **Spring Data JPA + Hibernate**
- **PostgreSQL** (banco local em desenvolvimento)
- **Flyway** (desabilitado por enquanto — Hibernate cria as tabelas via `ddl-auto=create-drop`)
- **Lombok**
- **Maven**

---

## Estrutura de pacotes esperada

```
com.barbearia.api
├── config/          # SecurityConfig e configurações gerais
├── entity/          # Entidades JPA
├── enums/           # Enums do domínio
├── repository/      # Interfaces Spring Data
├── service/         # Regras de negócio
├── controller/      # Endpoints REST
└── dto/             # Objetos de entrada e saída das APIs
```

---

## Entidades — 5 no total

### BARBEIRO
Usuário profissional do sistema. Tem login próprio.

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gerado automaticamente |
| nome | String | não nulo |
| email | String | não nulo, único — usado para login |
| senha | String | não nulo |
| horario_funcionamento | String | nullable |
| criado_em | Instant | não nulo, imutável, preenchido no @PrePersist |

---

### CLIENTE
Pessoa atendida pela barbearia. Pode ou não ter conta no sistema.

| Campo             | Tipo | Restrições |
|-------------------|---|---|
| id                | UUID | PK, gerado automaticamente |
| barbeiro_id       | UUID | FK → BARBEIRO, não nulo |
| nome              | String | não nulo |
| email             | String | **nullable** — só obrigatório no autocadastro |
| telefone          | String | não nulo |
| senha             | String | **nullable** — só obrigatório no autocadastro |
| cadastro_completo | boolean | não nulo, default false |
| criado_em         | Instant | não nulo, imutável, preenchido no @PrePersist |

**Regra crítica:** `email` e `senha` são nullable no banco porque existem dois caminhos de criação:
1. **Autocadastro pelo cliente** → email e senha são obrigatórios. Validação na camada de serviço/DTO, não na entidade.
2. **Cadastro pelo barbeiro** (cliente sem internet) → só nome + telefone. email e senha ficam null, `cadastro_completo = false`.

Se um cliente cadastrado pelo barbeiro quiser criar conta depois, ele "reivindica" o registro existente pelo telefone — não cria um novo.

O método `podeLogar()` deve retornar `true` apenas se `cadastroCompleto == true && email != null && senha != null`.

---

### SERVICO
Serviço oferecido pela barbearia (corte, barba, combo, etc).

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gerado automaticamente |
| barbeiro_id | UUID | FK → BARBEIRO, não nulo |
| nome | String | não nulo |
| preco | BigDecimal | não nulo, positivo |
| duracao_minutos | Integer | não nulo, positivo |
| custo_insumo | BigDecimal | não nulo, default 0 |
| ativo | boolean | não nulo, default true |

**Nota:** `custo_insumo` é um valor fixo cadastrado manualmente (ex: R$ 3 de pomada por corte). Não é controle de estoque — isso fica para versão futura.

---

### AGENDAMENTO
Registro de um atendimento marcado.

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gerado automaticamente |
| barbeiro_id | UUID | FK → BARBEIRO, não nulo |
| cliente_id | UUID | FK → CLIENTE, não nulo |
| servico_id | UUID | FK → SERVICO, não nulo |
| data_hora | LocalDateTime | não nulo |
| status | StatusAgendamento | não nulo, default CONFIRMADO |
| remarcado_para_id | UUID | FK → AGENDAMENTO (self-reference), **nullable** |
| criado_em | Instant | não nulo, imutável, preenchido no @PrePersist |

---

### MOVIMENTO_FINANCEIRO
Registro financeiro gerado quando um agendamento é concluído. Congela os valores do momento da conclusão.

| Campo | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gerado automaticamente |
| agendamento_id | UUID | FK → AGENDAMENTO, não nulo, único |
| valor_bruto | BigDecimal | não nulo |
| custo_insumo | BigDecimal | não nulo |
| lucro_liquido | BigDecimal | não nulo — calculado no @PrePersist |
| data | LocalDate | não nulo |

**Regra:** `lucro_liquido = valor_bruto - custo_insumo`. Calculado automaticamente no `@PrePersist` se não foi informado.

---

## Enum StatusAgendamento

```
CONFIRMADO  → estado inicial ao criar o agendamento
CONCLUIDO   → barbeiro marcou como atendido → GERA MovimentoFinanceiro
CANCELADO   → cliente ou barbeiro cancelou → NÃO gera MovimentoFinanceiro
NO_SHOW     → cliente não apareceu → NÃO gera MovimentoFinanceiro
REMARCADO   → horário foi editado → NÃO gera MovimentoFinanceiro
```

**Regra de remarcação:** ao editar o horário de um agendamento, o registro atual vira `REMARCADO` (com `remarcado_para_id` apontando pro novo) e um novo `AGENDAMENTO` é criado com o horário atualizado. Preserva histórico de remarcações sem contar como cancelamento.

---

## Relacionamentos

```
BARBEIRO    (1) ──── (N) CLIENTE
BARBEIRO    (1) ──── (N) SERVICO
BARBEIRO    (1) ──── (N) AGENDAMENTO
CLIENTE     (1) ──── (N) AGENDAMENTO
SERVICO     (1) ──── (N) AGENDAMENTO
AGENDAMENTO (1) ──── (0..1) MOVIMENTO_FINANCEIRO
AGENDAMENTO (1) ──── (0..1) AGENDAMENTO via remarcado_para_id (self-reference)
```

---

## Regras de negócio — checklist de validação

Ao revisar o código, confirme que cada uma dessas regras está implementada:

- [ ] `senha` e `email` de CLIENTE são nullable na entidade JPA
- [ ] A obrigatoriedade de email e senha no autocadastro está no DTO/service, não na entidade
- [ ] `cadastro_completo` começa como `false` por default
- [ ] `podeLogar()` só retorna `true` se `cadastroCompleto && email != null && senha != null`
- [ ] `StatusAgendamento` tem exatamente 5 valores: CONFIRMADO, CONCLUIDO, CANCELADO, NO_SHOW, REMARCADO
- [ ] `MovimentoFinanceiro` só é criado quando status = CONCLUIDO (validar no service)
- [ ] NO_SHOW e REMARCADO não geram MovimentoFinanceiro
- [ ] `lucro_liquido` é calculado automaticamente no `@PrePersist` de MovimentoFinanceiro
- [ ] Remarcação cria novo Agendamento e marca o antigo como REMARCADO com `remarcado_para_id` preenchido
- [ ] `custo_insumo` em SERVICO tem default 0 (não pode ser null)
- [ ] `ativo` em SERVICO tem default true
- [ ] Todos os `criado_em` são preenchidos no `@PrePersist` e marcados como `updatable = false`
- [ ] FKs lazy: todos os `@ManyToOne` usam `fetch = FetchType.LAZY`
- [ ] CLIENTE tem `barbeiro_id` como FK obrigatória (não nulo)

---

## Configurações do ambiente

### application.properties (src/main/resources)
```properties
spring.application.name=api
spring.datasource.url=jdbc:postgresql://localhost:5432/barbearia
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.flyway.enabled=false
```
---

## O que ainda não foi implementado (próximos passos)

1. **Repositories** — um por entidade, começando com `JpaRepository` simples
2. **Services** — regras de negócio (criar agendamento, mudar status, gerar MovimentoFinanceiro)
3. **DTOs** — objetos de entrada/saída das APIs (separar do que a entidade expõe)
4. **Controllers** — endpoints REST
5. **JWT** — autenticação do barbeiro e do cliente
6. **Flyway migrations** — substituir `create-drop` quando o schema estabilizar

---

## Roadmap futuro (fora do MVP atual)

- Multi-tenant: entidade `BARBEARIA` separando barbeiros e clientes por tenant
- Entidade `ASSINATURA` para controle de planos SaaS
- `CLIENTE.barbeiro_id` vira `CLIENTE.barbearia_id`
- Controle de estoque de insumos
- Mapa de calor e taxa de retenção de clientes
- Gateway de pagamento
- App nativo (iOS/Android)