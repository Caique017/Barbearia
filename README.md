# Barbearia API · ClipBook

API REST do **ClipBook**, sistema de agendamento e gestão para o **barbeiro que trabalha sozinho**.

- O cliente agenda por um link público da barbearia, com os horários livres calculados aqui no backend.
- O barbeiro cuida da agenda, da carteira de clientes e dos serviços.
- O barbeiro vê quanto dinheiro **realmente** sobrou no mês: faturamento menos o custo de insumos.

O front fica em [Caique017/Barbearia-web](https://github.com/Caique017/Barbearia-web).

> **Status (out/2026):** API completa para a V1, com todas as issues fechadas e 397 testes verdes. O CI roda em todo PR. A infraestrutura AWS está escrita em Terraform e o deploy automático está pronto, mas **ainda não foi aplicado**.

---

## Sumário

- [O que a API faz](#o-que-a-api-faz)
- [Stack](#stack)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints](#endpoints)
- [Regras de negócio](#regras-de-negócio)
- [Assinatura (Stripe)](#assinatura-stripe)
- [Segurança](#segurança)
- [Modelo de dados](#modelo-de-dados)
- [Estrutura do código](#estrutura-do-código)
- [Testes](#testes)
- [CI/CD e infraestrutura](#cicd-e-infraestrutura)
- [Decisões de projeto](#decisões-de-projeto)
- [Limitações conhecidas](#limitações-conhecidas)

---

## O que a API faz

| Área | Funcionalidades |
|---|---|
| **Conta** | Cadastro e login de barbeiro e cliente; esqueci/redefinir senha por e-mail; "sair de todos os aparelhos" |
| **Barbearia** | Perfil, nome da barbearia, horário em texto, **link público por slug** (gerado ou escolhido) |
| **Carteira** | Cadastro rápido de cliente pelo barbeiro; edição; **pedido de vínculo** quando alguém cria conta com um telefone que já está na carteira |
| **Serviços** | Cadastro com preço, duração e custo de insumo; ativar e desativar; lista pública sem o custo |
| **Horários** | Faixas de atendimento por dia da semana (com intervalo de almoço) e **bloqueios** (folga, feriado, férias, uma hora fechada) |
| **Disponibilidade** | Horários livres de um dia para um serviço, de 15 em 15 minutos, até 60 dias à frente |
| **Agenda** | Agendamento pelo barbeiro (pode ser fora do expediente) e pelo cliente; concluir, faltou, cancelar, remarcar |
| **Financeiro** | Movimento gerado ao concluir; resumo por período com faturamento, custos, lucro, resumo por dia e **mix de serviços** |
| **Assinatura** | 7 dias grátis + R$ 29,99/mês pelo Stripe: **cartão recorrente** ou **Pix de 30 dias**; vencido = só leitura (402) |

## Stack

| | |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 4.1.1: Web MVC, Data JPA, Security, Validation, Mail, Actuator |
| Banco | PostgreSQL 17, com a extensão `btree_gist` |
| Migrations | Flyway (`V1` a `V11`), com Hibernate em `ddl-auto=validate` |
| Autenticação | JWT stateless (auth0 java-jwt 4.6.2, HMAC256) + BCrypt |
| Pagamentos | Stripe Java 34 (Checkout, Billing, Customer Portal, webhooks) |
| JSON | Jackson 3 (`tools.jackson.databind`) |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers |
| Build | Maven (wrapper incluso), imagem Docker multi-stage |
| Infra | Terraform (AWS), GitHub Actions com OIDC |

---

## Como rodar

**Pré-requisitos:**
- JDK 21;
- PostgreSQL em `localhost:5432` com o banco `barbearia`;
- Docker, só para os testes de integração e para o Mailpit.

```bash
# 1. banco
createdb -U postgres barbearia

# 2. segredo do JWT — obrigatório, mínimo 32 bytes; sem ele a API não sobe
export API_SECURITY_TOKEN_SECRET="$(openssl rand -base64 48)"

# 3. (opcional) caixa de e-mail falsa para o "esqueci a senha"
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

# 4. sobe na porta 8080
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Na subida, o Flyway aplica as migrations que faltam e o Hibernate só **valida** o schema. Se uma entidade não bater com o banco, a aplicação não sobe, e isso é de propósito.

Conferir:

```bash
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

Com o front local (`npm run dev` no Barbearia-web), o Vite faz proxy de `/api/*` para `localhost:8080`. Os e-mails do Mailpit aparecem em `http://localhost:8025`.

### Imagem Docker

```bash
docker build -t barbearia-api .
```

É a mesma imagem que vai para produção: multi-stage, roda sem root e com JRE 21.

## Variáveis de ambiente

| Variável | Obrigatória | Padrão (dev) | Para quê |
|---|---|---|---|
| `API_SECURITY_TOKEN_SECRET` | **sim** | — | assinatura dos JWT (≥ 32 bytes) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | não | `localhost:5432/barbearia`, `postgres`, `admin` | banco |
| `MAIL_HOST` / `MAIL_PORT` | não | `localhost` / `1025` | SMTP (Mailpit em dev, SES em produção) |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS`, `MAIL_REMETENTE` | não | vazio / `false` | SMTP autenticado |
| `FRONT_URL` | não | `http://localhost:5173` | links do e-mail e retorno do Stripe |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_ID` | não | vazio | sem elas, o checkout responde **503** e o resto funciona |
| `CORS_ORIGENS` | não | vazio | origens de outro domínio liberadas (vazio = nenhuma) |
| `PROXY_CONFIAVEL` | não | `false` | usar o `X-Forwarded-For` (atrás do CloudFront) |
| `ORIGEM_SEGREDO` | não | vazio | header que só o CloudFront conhece (vazio = desligado) |
| `SHOW_SQL` | não | `false` | loga o SQL do Hibernate |

---

## Endpoints

`🌐` público · `💈` barbeiro · `👤` cliente · `🔑` qualquer usuário logado

Autenticação por `Authorization: Bearer <token>`. São 46 rotas, mais o `/actuator/health`.

### Conta — `/auth`

| | Método | Rota | O que faz |
|---|---|---|---|
| 🌐 | POST | `/auth/barbeiro/registro` | Cria usuário + barbearia, gera o slug e inicia os 7 dias de teste. Devolve token e slug |
| 🌐 | POST | `/auth/barbeiro/login` | Login do barbeiro |
| 🌐 | POST | `/auth/cliente/registro` | Conta de cliente numa barbearia (`barbeiroId`). Se o telefone já está na carteira, vira **pedido de vínculo** |
| 🌐 | POST | `/auth/cliente/login` | Login do cliente |
| 🔑 | GET | `/auth/me` | Dados do usuário do token |
| 🔑 | POST | `/auth/sair` | Encerra **todas** as sessões (tokens antigos param de valer) |
| 🌐 | POST | `/auth/senha/esqueci` | Envia o link de redefinição. Responde 204 sempre, exista o e-mail ou não |
| 🌐 | POST | `/auth/senha/redefinir` | Troca a senha com o token do link (uso único, 30 min) |

### Barbearia — `/barbeiros`

| | Método | Rota | O que faz |
|---|---|---|---|
| 🌐 | GET | `/barbeiros/publico/{slug}` | Dados públicos: `barbeiroId`, nome, horário em texto |
| 🌐 | GET | `/barbeiros/publico/{slug}/servicos` | Serviços **ativos**, sem o custo de insumo |
| 🌐 | GET | `/barbeiros/publico/{slug}/disponibilidade?servicoId&data` | Horários livres do dia para o serviço |
| 💈 | GET / PATCH | `/barbeiros/me` | Perfil (com e-mail) / editar nome da barbearia e horário em texto |
| 💈 | PATCH | `/barbeiros/me/slug` | Escolher o próprio link (**409** se reservado ou ocupado) |
| 💈 | GET / PUT | `/barbeiros/me/horarios` | Faixas de atendimento / substituir todas |
| 💈 | GET / POST | `/barbeiros/me/bloqueios` | Listar / criar bloqueio (a resposta conta os agendamentos que caem nele) |
| 💈 | DELETE | `/barbeiros/me/bloqueios/{id}` | Remover bloqueio |

### Carteira — `/clientes` 💈

| Método | Rota | O que faz |
|---|---|---|
| POST | `/clientes` | Cadastro rápido (`nome` + `telefone`) |
| GET | `/clientes` | Carteira, com os pedidos de vínculo pendentes |
| GET / PATCH | `/clientes/{id}` | Detalhe / corrigir nome e telefone |
| POST | `/clientes/{id}/vinculo` | Aprova o pedido: a conta passa a ser deste cliente |
| DELETE | `/clientes/{id}/vinculo` | Recusa o pedido e apaga a conta que pediu |

### Serviços — `/servicos` 💈

| Método | Rota | O que faz |
|---|---|---|
| POST | `/servicos` | Cria (nome único por barbearia, duração de 5 a 480 min) |
| GET | `/servicos` | Lista todos, ativos e inativos |
| GET | `/servicos/{id}` | Detalhe |
| PATCH | `/servicos/{id}` | Edição parcial, inclusive `ativo` |

### Agenda do barbeiro 💈

| Método | Rota | O que faz |
|---|---|---|
| GET | `/agenda?de&ate` | Agendamentos do período (padrão: hoje) |
| POST | `/agenda` | Agenda um cliente da carteira (**pode** ser fora do expediente) |
| GET | `/agenda/{id}` | Detalhe |
| POST | `/agenda/{id}/concluir` | Conclui (só depois do horário) e gera o movimento financeiro |
| POST | `/agenda/{id}/falta` | Marca falta (só depois do horário) |
| POST | `/agenda/{id}/cancelar` | Cancela |
| POST | `/agenda/{id}/remarcar` | Cria um agendamento novo; o antigo vira `REMARCADO` |
| GET | `/financeiro/resumo?de&ate` | Totais, resumo por dia e mix por serviço (padrão: mês atual, até 366 dias) |

### Agendamentos do cliente 👤

| Método | Rota | O que faz |
|---|---|---|
| GET | `/agendamentos/minha-barbearia` | Slug, nome e horário da barbearia do cliente |
| GET | `/agendamentos` | Agendamentos do cliente |
| POST | `/agendamentos` | Agenda (exige vínculo aprovado e horário **dentro** do expediente) |
| POST | `/agendamentos/{id}/cancelar` | Cancela (até o horário começar) |
| POST | `/agendamentos/{id}/remarcar` | Remarca (até o horário começar) |

### Assinatura

| | Método | Rota | O que faz |
|---|---|---|---|
| 💈 | GET | `/assinatura` | Status, se está ativa, dias restantes, forma de pagamento, preço |
| 💈 | POST | `/assinatura/checkout` | `{formaPagamento: CARTAO \| PIX}` → `{url}` do Stripe Checkout |
| 💈 | POST | `/assinatura/portal` | `{url}` do portal do Stripe (cartão, cancelamento, faturas) |
| 🌐 | POST | `/pagamentos/webhook` | Eventos do Stripe, com assinatura conferida |

### Formato de erro

Todo erro sai no mesmo envelope:

```json
{
  "timestamp": "2026-10-21T03:49:45Z",
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Esse horário não está mais livre. Escolha outro.",
  "caminho": "/agenda",
  "campos": null
}
```

| Status | Quando |
|---|---|
| 400 | validação: `campos` traz `campo` e `mensagem` de cada erro |
| 401 | sem token, token inválido ou expirado, sessão encerrada, login errado |
| 402 | assinatura vencida: escritas do barbeiro, ou cliente agendando numa barbearia vencida |
| 403 | papel errado (cliente numa rota de barbeiro e vice-versa) |
| 404 | não existe **ou é de outro barbeiro** |
| 405 / 415 | método ou content-type errado |
| 409 | horário ocupado, slug em uso, nome de serviço repetido, transição de status inválida, vínculo pendente |
| 429 | limite de login ou de requisições por IP, com `Retry-After` |
| 503 | Stripe não configurado |

---

## Regras de negócio

### Disponibilidade

Um horário está livre quando:
1. está no futuro e a no máximo 60 dias;
2. o serviço **cabe inteiro** numa faixa de atendimento daquele dia da semana;
3. não sobrepõe agendamento `CONFIRMADO` nem `CONCLUIDO`;
4. não sobrepõe bloqueio.

Os candidatos vêm de 15 em 15 minutos. O cálculo está numa classe pura, a `CalculadoraDisponibilidade`, sem Spring e sem banco, com cada caso de borda testado. Tudo é **hora de parede** da barbearia (`America/Sao_Paulo`), então o servidor pode rodar em UTC.

### Agendamento

- **Barbeiro:** pode encaixar fora do expediente. Agendamento sobreposto, bloqueio e horário no passado continuam barrados.
- **Cliente:** precisa de conta com **vínculo aprovado** e de um horário dentro do expediente.
- **Duas pessoas no mesmo horário:** a constraint `EXCLUDE USING gist (barbeiro_id WITH =, tsrange(data_hora, termina_em) WITH &&)` faz o Postgres recusar a sobreposição. Isso vale mesmo quando duas requisições chegam juntas.
- **Preço da época:** o agendamento guarda preço, custo e duração da hora em que foi marcado. Mudar o preço do serviço não altera a agenda nem o financeiro passados.

```
CONFIRMADO ──concluir──▶ CONCLUIDO   (só depois do horário · gera o financeiro)
           ──falta─────▶ NO_SHOW     (só depois do horário)
           ──cancelar──▶ CANCELADO   (cliente: só antes de começar)
           ──remarcar──▶ REMARCADO   (cria um novo e aponta para ele)
```

Status final não muda mais. As regras vivem na entidade `Agendamento`.

### Financeiro

Concluir gera um `MovimentoFinanceiro` com valor bruto, insumo e líquido, na mesma transação (idempotente, com `agendamento_id` único). O resumo agrupa por dia e por serviço; o mix vem do serviço mais feito para o menos feito.

## Assinatura (Stripe)

- **Teste:** 7 dias grátis a partir do cadastro, sem cartão.
- **Cartão:** assinatura recorrente de R$ 29,99/mês (Stripe Billing).
- **Pix:** pagamento avulso que libera **30 dias**, somados ao que ainda resta. O Pix Automático não existe para conta brasileira do Stripe; por isso há um e-mail de lembrete 3 dias antes de vencer.
- **Acesso** decidido por datas: `agora < testeAte` ou `agora < acessoAte`.
- **Vencido = só leitura:** o `AssinaturaInterceptor` devolve 402 nas escritas do barbeiro. Nada é apagado.
- **Webhook idempotente:** cada evento processado fica em `evento_pagamento`, e o Stripe pode reenviar à vontade.
- **Inversão de dependência:** a regra conhece só a interface `GatewayPagamento`; o SDK fica isolado no `StripeGateway`.

Para configurar o Stripe (produto, preço, Pix, portal e webhook), veja a nota *16 — Assinatura e Pagamentos* do cofre do projeto, ou a seção de produção abaixo.

---

## Segurança

| Camada | O que faz |
|---|---|
| **JWT + versão de sessão** | O token carrega o claim `sv`. Sair e trocar a senha incrementam a versão, e os tokens antigos morrem na hora. Expira em 2h |
| **Usuário recarregado do banco** | O claim `role` não autoriza nada sozinho; o filtro relê o usuário a cada request |
| **Duas barreiras de autorização** | O `SecurityConfig` decide se a request entra; o `@PreAuthorize` em cada controller decide se o método roda |
| **Dono resolvido na consulta** | `findByIdAndBarbeiroId`: o registro de outro barbeiro devolve **404**, nunca 403 |
| **Login vago** | A mesma mensagem e o mesmo tempo (BCrypt sempre roda) para e-mail inexistente, senha errada e papel errado |
| **Limites** | 5 falhas por e-mail em 15 min (login); por IP nas rotas públicas de login, cadastro e senha |
| **Esqueci a senha** | Token de 256 bits; só o **hash** fica no banco; uso único com update condicional; e-mail enviado **depois do commit**; job limpa os tokens vencidos |
| **Headers** | CSP `default-src 'none'`, HSTS, `Referrer-Policy: no-referrer`, `nosniff`, `X-Frame-Options: DENY` |
| **CORS fechado** | Nada liberado por padrão; em produção, só a origem do próprio site |
| **Atrás do CloudFront** | `OrigemConfiavelFilter` exige um header secreto; o IP do cliente vem do último salto do `X-Forwarded-For` |
| **Logs de segurança** | `evento=login_falhou`, `limite_ip_excedido`, `acesso_negado`... com e-mail mascarado (`j***@ex.com`), viram métricas e alarmes |
| **Senhas** | BCrypt, limite de 72 bytes validado (`@MaxBytes`) |

---

## Modelo de dados

```
usuario ─1:1─ barbeiro ─1:N─ cliente ─(usuario_id opcional)─ usuario
                 │
                 ├─1:N─ servico
                 ├─1:N─ barbeiro_horario      (faixas por dia da semana)
                 ├─1:N─ bloqueio_horario
                 ├─1:1─ assinatura
                 └─1:N─ agendamento ─N:1─ cliente, servico
                              └─1:1─ movimento_financeiro

token_redefinicao_senha ─N:1─ usuario
evento_pagamento                       (ids de webhook já processados)
```

- **`usuario`**: identidade e login. E-mail único (sempre minúsculo), papel, BCrypt e `versao_sessao`.
- **`cliente`**: o `usuario_id` é **opcional**. O cliente pode existir só na carteira e ganhar conta depois. O telefone é único **por barbeiro**.
- **`agendamento`**: `data_hora` e `termina_em`, mais preço e custo da época, `remarcado_para` e a constraint `EXCLUDE`.

| Migration | O que muda |
|---|---|
| V1 | schema inicial |
| V2 | slug do barbeiro |
| V3 | nome próprio da barbearia |
| V4 | vínculo pendente de cliente |
| V5 | e-mail minúsculo |
| V6 | token de redefinição de senha |
| V7 | versão de sessão |
| V8 | regras de serviço |
| V9 | agendamento, horário de atendimento e financeiro (`btree_gist`) |
| V10 | bloqueios |
| V11 | assinatura e eventos de pagamento |

## Estrutura do código

```
com.barbearia.api
├── config/        AplicacaoConfig (relógio, fuso), WebConfig, AssinaturaInterceptor (402)
├── controller/    Auth, RedefinicaoSenha, Barbeiro, Cliente, Servico, Agenda, Agendamento, Assinatura
├── domain/        entidades JPA, com as regras de estado (Agendamento, Assinatura)
├── dto/           records de request e response, por assunto
├── enums/         Role, StatusAgendamento, StatusAssinatura, FormaPagamento
├── exceptions/    exceções de negócio + GlobalExceptionHandler
├── repositories/  Spring Data JPA
├── security/      SecurityConfig, filtros (JWT, origem, limite por IP), TokenService, eventos
├── service/       regras de negócio, CalculadoraDisponibilidade, GatewayPagamento/StripeGateway, jobs
├── util/          Emails, Telefone
└── validation/    @MaxBytes
```

Organização **por camada**, decisão consciente para o tamanho atual do projeto.

### O caminho de uma requisição

```
OrigemConfiavelFilter        header secreto do CloudFront (produção)
LimiteRequisicoesPorIpFilter rotas públicas sensíveis → 429
SecurityFilter               Bearer → assinatura, validade e versão de sessão → usuário do banco
SecurityConfig               rota pública passa; senão exige autenticado → 401
AssinaturaInterceptor        escrita de barbeiro com assinatura vencida → 402
@PreAuthorize                papel errado → 403
@Valid                       corpo fora do formato → 400 com os campos
Service                      regra de negócio em @Transactional (dono resolvido na consulta → 404)
GlobalExceptionHandler       qualquer exceção vira o envelope padrão
```

---

## Testes

```bash
./mvnw test       # 220 unitários — sem banco, sem Docker, segundos
./mvnw verify     # + 177 de integração com PostgreSQL 17 real (Docker aberto)
```

- **Unitários (`*Test`):** regras puras (calculadora, entidades, e-mail, telefone), services com Mockito, filtros de segurança e validação dos DTOs. O "agora" é sempre um `Clock` injetável.
- **Integração (`*IT`):** `@SpringBootTest` + MockMvc contra um Postgres de verdade (Testcontainers), que nunca é o seu banco local. Cobrem:
  - a **matriz de segurança**: toda rota protegida contra sem token, token lixo e papel errado;
  - os fluxos completos de agenda, bloqueios, vínculo e redefinição de senha;
  - a constraint `EXCLUDE` direto no banco;
  - o webhook assinado com HMAC como o Stripe faz;
  - as migrations rodando sobre dados que já existiam.

O **nome da classe** decide onde o teste roda: `*Test` no Surefire, `*IT` no Failsafe. Uma classe fora desse padrão não roda no Maven nem no CI.

## CI/CD e infraestrutura

### CI (`.github/workflows/ci.yml`), em todo PR e push no `master`

- `./mvnw verify`: os 397 testes, com Postgres via Testcontainers;
- `docker build` da imagem;
- `terraform fmt -check` + `validate`;
- *dependency review*, que falha em vulnerabilidade alta.

Dependabot semanal (Maven, Actions, Docker, Terraform), com a versão principal do Java travada na LTS. Secret scanning e push protection ligados.

### Produção na AWS (`infra/`, Terraform)

```
Navegador ─HTTPS─▶ CloudFront ─┬─▶ S3 (front, via OAC)
                               └─/api/*─▶ EC2 t3.small (container da API) ─TLS─▶ RDS PostgreSQL 17
```

- **EC2:** Amazon Linux 2023, IMDSv2, disco criptografado, **sem SSH** (acesso pelo SSM). A porta 8080 só aceita a *prefix list* do CloudFront.
- **RDS:** subnet privada, criptografado, `force_ssl`, backup de 7 dias e senha gerenciada pelo Secrets Manager.
- **Segredos:** no SSM Parameter Store. JWT e segredo de origem são gerados pelo Terraform; SMTP e Stripe são preenchidos à mão.
- **Monitoramento:** alarmes no CloudWatch (instância fora, disco do banco, picos de login falho, limite por IP, acesso negado, erros) e orçamento mensal, tudo por e-mail via SNS.

**Deploy (`deploy.yml`):** roda depois do CI verde no `master`.
1. Login na AWS por **OIDC**, sem chave guardada.
2. Build e push no ECR, com a tag igual ao SHA do commit.
3. `deploy.sh` na EC2 pelo SSM, que espera o health check por até 3 minutos.

O job só roda depois que a variável `AWS_ROLE_ARN` é configurada no repositório.

**Colocar no ar:**
1. `cd infra && terraform init && terraform apply`.
2. Confirmar os e-mails do SNS e do SES.
3. Preencher os parâmetros `PREENCHER-...` no SSM: SMTP e as três chaves do Stripe.
4. Criar o environment `producao` e as variáveis do `terraform output` nos dois repositórios.
5. Rodar o Deploy.

---

## Decisões de projeto

- **O barbeiro nunca vem do body.** Ele sai sempre do token, e a posse do registro é resolvida na própria consulta.
- **A rota pública tem prefixo próprio** (`/barbeiros/publico/...`). Assim não existe curinga no Security que alcance o `/barbeiros/me`, que devolve e-mail.
- **DTO público separado, sempre.** O `BarbeiroPublicoResponse` não reaproveita o perfil, porque o perfil tem e-mail.
- **Telefone não prova identidade.** Conta criada com um telefone que já está na carteira vira pedido de vínculo, não ligação automática.
- **Slug derivado sufixa em silêncio** (`-2`, `-3`); o slug escolhido devolve 409. Quem barra a corrida é a constraint do banco.
- **A disponibilidade é calculada no backend.** O front não decide se um horário está livre; a constraint `EXCLUDE` é a última barreira.
- **Agendamento online exige conta.** O fluxo "agendar sem conta" do documento de produto ficou para depois, e vai precisar de confirmação por código.
- **Vencer a assinatura não apaga nada.** A conta fica só para leitura; o barbeiro nunca perde a agenda.

## Limitações conhecidas

- **Ainda não está no ar.** O Terraform está pronto, mas nenhum recurso foi criado.
- **Trecho CloudFront → EC2 em HTTP.** Ele é fechado por IP e por header secreto, mas TLS até a origem exige domínio próprio.
- **Limites em memória.** Valem por instância e zeram ao reiniciar; com mais de uma instância, precisam de Redis ou WAF.
- **Uma EC2 só.** Não há alta disponibilidade.
- **O cadastro revela se o e-mail existe** (409). O limite por IP reduz o estrago.
- **Token de 2h sem refresh.**
- **Sem "arquivar cliente".** Apagar um cliente com agendamentos esbarra na FK.
- **O e-mail é único no sistema.** Um cliente de duas barbearias precisa de duas contas.
- **SES em sandbox** até pedir acesso de produção. Até lá, o "esqueci a senha" só entrega para endereços verificados.
