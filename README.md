# Barbearia API

API REST para gestão de barbearia: o barbeiro mantém a carteira de clientes e
compartilha um **link público de cadastro** (`/barbeiros/publico/joao-silva`) que
o cliente abre já sabendo de qual barbearia se trata — sem digitar UUID nenhum.

> **Status:** em desenvolvimento. Autenticação, carteira de clientes e link
> público estão funcionando. Agendamento e financeiro têm as tabelas criadas, mas
> ainda não têm regra de negócio. Veja o [roadmap](#roadmap).

## Stack

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.0 (Web MVC, Data JPA, Security, Validation) |
| Banco | PostgreSQL 17 |
| Migrations | Flyway |
| Auth | JWT stateless (auth0 java-jwt 4.4.0) + BCrypt |
| JSON | Jackson 3 (`tools.jackson.databind`) |
| Build | Maven (wrapper incluso) |

## Como rodar

**Pré-requisitos:** JDK 21+ e PostgreSQL rodando em `localhost:5432`.

```bash
# 1. crie o banco
createdb -U postgres barbearia

# 2. defina o segredo do JWT (obrigatório, mínimo 32 bytes)
export API_SECURITY_TOKEN_SECRET="$(openssl rand -base64 48)"

# 3. suba
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O Flyway aplica as migrations no boot e o
Hibernate valida se o schema bate com as entidades — se divergir, a aplicação
**não sobe**, e é de propósito. O mesmo vale para o segredo do JWT: sem ele, ou
com menos de 32 bytes, a aplicação se recusa a subir, em vez de assinar tokens
com um valor que está no repositório.

Usuário e senha do banco estão em `src/main/resources/application.properties`
(`postgres`/`admin`, valores de desenvolvimento). Ajuste para o seu ambiente.

## Endpoints

`🔓` = público · `🔒` = exige `Authorization: Bearer <token>`

### `/auth`

| | Método | Rota | O que faz |
|---|---|---|---|
| 🔓 | POST | `/auth/barbeiro/registro` | Cria usuário BARBEIRO + perfil e gera o slug a partir do `nomeBarbearia`. Devolve token e slug |
| 🔓 | POST | `/auth/barbeiro/login` | Valida credenciais e role. Devolve token |
| 🔓 | POST | `/auth/cliente/registro` | Cria usuário CLIENTE no barbeiro informado. Se o telefone já está na carteira, vira **pedido de vínculo** |
| 🔓 | POST | `/auth/cliente/login` | Valida credenciais e role. Devolve token |
| 🔒 | GET | `/auth/me` | Dados do usuário do token |

Login inválido devolve **401**; 5 falhas seguidas no mesmo e-mail bloqueiam o
login por 15 minutos (**429** com `Retry-After`).

### `/barbeiros`

| | Método | Rota | O que faz |
|---|---|---|---|
| 🔓 | GET | `/barbeiros/publico/{slug}` | Dados públicos da barbearia: `barbeiroId`, `nome`, `horarioFuncionamento` |
| 🔒 | GET | `/barbeiros/me` | Perfil do barbeiro logado — **inclui e-mail** |
| 🔒 | PATCH | `/barbeiros/me` | Edita `nomeBarbearia` e/ou `horarioFuncionamento`. Não mexe no slug |
| 🔒 | PATCH | `/barbeiros/me/slug` | O barbeiro escolhe o próprio link. **409** se reservado ou ocupado |

### `/clientes` *(todos exigem role BARBEIRO)*

| | Método | Rota | O que faz |
|---|---|---|---|
| 🔒 | POST | `/clientes` | Cadastro rápido pelo barbeiro (`nome` + `telefone`) |
| 🔒 | GET | `/clientes` | Lista a carteira do barbeiro logado |
| 🔒 | GET | `/clientes/{id}` | Detalhe de um cliente da própria carteira |
| 🔒 | PATCH | `/clientes/{id}` | Corrige `nome` e/ou `telefone` |
| 🔒 | POST | `/clientes/{id}/vinculo` | Aprova o pedido de vínculo da conta ao cliente |
| 🔒 | DELETE | `/clientes/{id}/vinculo` | Recusa o pedido e apaga a conta que pediu |

### Formato de erro

Todo erro sai no mesmo envelope:

```json
{
  "timestamp": "2026-08-13T00:45:53Z",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Barbearia não encontrada.",
  "caminho": "/barbeiros/publico/nao-existe",
  "campos": null
}
```

`campos` só aparece em erro de validação, com o par `campo`/`mensagem`.

## Modelo de dados

```
usuario  ──1:1──  barbeiro  ──1:N──  cliente
   │                  │                 │
   │ (opcional)       │                 │
   └──────────1:1─────┼─────────────────┘
                      │
                      ├──1:N──  servico
                      │
                 agendamento ──1:1── movimento_financeiro
```

- **`usuario`** — identidade e login. Implementa `UserDetails`, e-mail único,
  role `BARBEIRO`|`CLIENTE`, senha em BCrypt
- **`barbeiro`** — perfil profissional, 1:1 com usuário. Tem o `slug` único do
  link público
- **`cliente`** — carteira do barbeiro. `usuario_id` é **nullable**: o cliente
  pode existir sem conta (cadastrado pelo barbeiro) e ganhar conta depois
- **`servico`**, **`agendamento`**, **`movimento_financeiro`** — tabelas e
  entidades prontas, sem regra de negócio ainda

Telefone é único **por barbeiro** (`uk_cliente_barbeiro_telefone`), não global —
o mesmo cliente pode existir em duas barbearias.

## Decisões de projeto

Algumas escolhas que não são óbvias pelo código:

**O barbeiro nunca vem do body.** Nos endpoints autenticados ele sai sempre do
token (`@AuthenticationPrincipal`). A posse do registro se resolve na própria
consulta (`findByIdAndBarbeiroId`), não num `if` depois — cliente de outro
barbeiro devolve **404, nunca 403**, para não confirmar que aquele id existe.

**A rota pública tem prefixo próprio.** `/barbeiros/publico/{slug}` em vez de
`/barbeiros/{slug}`. Liberar o segundo formato no Security exigiria um curinga
`/barbeiros/*`, que pegaria o `/barbeiros/me` junto — e o `/me` devolve e-mail.
Com dois espaços de nome separados, não existe curinga capaz de alcançar o `/me`.

**Response de endpoint público é DTO separado, sempre.** O
`BarbeiroPublicoResponse` não reusa o `BarbeiroPerfilResponse` justamente porque
os campos são *quase* os mesmos — o perfil tem e-mail.

**Duas camadas independentes de autorização.** O `SecurityConfig` decide se a
requisição entra; o `@PreAuthorize` no controller decide se o método executa.
Passar numa não passa na outra.

**Slug é derivado, então conflito sufixa em silêncio.** O slug nasce do nome
(minúsculo, sem acento, não-alfanumérico vira hífen), com palavras reservadas
bloqueadas (`me`, `admin`, `api`, `publico`). Se colidir, vira `-2`, `-3`.
Ninguém pediu aquele slug específico, então sufixar não frustra. A checagem
antecipada nunca é atômica: quem barra duas requisições simultâneas é a
constraint do banco, e o handler devolve 409 em vez de 500.

**A mensagem de login é vaga de propósito.** `"E-mail ou senha incorretos."` para
e-mail inexistente, role errada e senha errada — e o BCrypt roda nos três casos,
para o tempo de resposta também não denunciar se o e-mail existe. (O cadastro
ainda responde "e-mail já está em uso", então isso dificulta, não impede, saber
quem tem conta.)

**Telefone não prova identidade.** Se alguém se cadastra como cliente com um
telefone que já está na carteira do barbeiro, a conta **não** é ligada ao
cliente automaticamente — vira um pedido que o barbeiro aprova ou recusa. Sem
isso, quem soubesse o telefone de outra pessoa assumiria o cadastro dela.

**E-mail é normalizado na entrada.** `Joao@Ex.com` e `joao@ex.com` são a mesma
conta: os records de request aplicam `trim` + minúsculo antes da validação.

**O claim `role` do token não autoriza nada.** O filtro recarrega o usuário do
banco a cada requisição e usa a role de lá. Mudança de permissão tem efeito
imediato, ao custo de um SELECT por request.

## Estrutura

```
com.barbearia.api
├── controller/     Auth, Barbeiro, Cliente
├── domain/         entidades JPA
├── dto/            records de request e response
├── enums/          Role, StatusAgendamento
├── exceptions/     exceptions de negócio + GlobalExceptionHandler
├── repositories/   Spring Data JPA
├── security/       SecurityConfig, SecurityFilter, TokenService,
│                   UsuarioDetailsService, SecurityErrorHandler
├── service/        regra de negócio
├── util/           Telefone, Emails
└── validation/     MaxBytes (limite de 72 bytes do BCrypt)
```

Package-by-layer, decisão consciente. Package-by-feature só compensa em projeto
maior — reavaliar quando o agendamento entrar.

## Testes

```bash
./mvnw test     # unitários: sem banco, sem Docker, poucos segundos
./mvnw verify   # unitários + integração (precisa do Docker rodando)
```

- **Unitários (`*Test`)** — JUnit 5, Mockito e AssertJ. Regras de slug,
  validações dos DTOs, `AuthService`, `TokenService`, limite de tentativas
- **Integração (`*IT`)** — `@SpringBootTest` + MockMvc contra um **PostgreSQL 17
  de verdade** subido pelo Testcontainers. Não usam o seu banco local. Cobrem a
  matriz de segurança (401/403 em toda rota protegida), os fluxos de perfil,
  slug, carteira e vínculo, e as migrations com linhas preexistentes

O CI (`.github/workflows/ci.yml`) roda `./mvnw verify` em todo push para
`master` e em todo pull request.

## Roadmap

- [x] Autenticação stateless com JWT
- [x] Cadastro e login de barbeiro e cliente
- [x] Carteira de clientes do barbeiro
- [x] Tratamento de erros padronizado
- [x] Schema versionado com Flyway
- [x] Link público de cadastro por slug
- [x] Nome próprio da barbearia + `PATCH` do slug
- [x] Extrair `AuthService`
- [x] Testes automatizados
- [ ] CRUD de serviços
- [ ] Agendamento
- [ ] Movimento financeiro
- [ ] CORS para o front

## Limitações conhecidas

Vale saber antes de usar isso como referência:

- **CORS não está configurado** — o navegador vai barrar a primeira chamada de um
  front em outra origem
- **Credenciais do banco estão no `application.properties`** com valores de
  desenvolvimento. Dá para sobrescrever com `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`
- **O limite de tentativas de login fica em memória** — vale por instância e
  zera quando a aplicação reinicia. Com mais de uma instância, precisa ir para
  um armazenamento compartilhado (Redis) ou para a borda (proxy/gateway)
- **Token expira em 2h e não há refresh token**
- **`Servico`, `Agendamento` e `MovimentoFinanceiro` não têm Lombok** — sem
  getters e setters, são inutilizáveis como estão
- **Cliente de duas barbearias precisa de duas contas, com e-mails diferentes** —
  o e-mail é único no sistema inteiro
