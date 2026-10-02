# Gerenciador de Chamados de Condomínio

Aplicação web para administração de condomínios. O sistema permite registrar e acompanhar chamados de moradores, organizar usuários e unidades e gerenciar reservas de áreas comuns.

## Funcionalidades

### Morador

- Abrir chamados para suas unidades e acompanhar o andamento.
- Consultar detalhes, adicionar comentários e anexar arquivos.
- Consultar áreas comuns, verificar disponibilidade, solicitar reservas e acompanhar o próprio histórico.
- Visualizar as próprias reservas em calendário e cancelar solicitações ou reservas elegíveis antes do horário de início.

### Colaborador

- Consultar chamados dentro do escopo de unidades e tipos de chamado atribuído.
- Acompanhar e atualizar chamados, com comentários e anexos.

### Administrador

- Gerenciar usuários, blocos, unidades e vínculos de moradores.
- Gerenciar tipos e status de chamados e definir o escopo de atuação dos colaboradores.
- Consultar chamados e acompanhar sua operação.
- Cadastrar, editar e desativar áreas comuns.
- Consultar reservas, aprovar ou negar solicitações com motivo e cancelar reservas elegíveis.
- Consultar reservas em calendário.

O acesso às funções é controlado pelo perfil autenticado. Colaboradores não participam do fluxo de reservas.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring MVC e JSP/JSTL
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Flyway para versionamento do banco
- Maven
- Docker e Docker Compose
- JUnit, Spring Boot Test, Testcontainers e JaCoCo para validação

## Requisitos para executar

- Docker Desktop instalado e em execução, com Docker Compose V2 disponível no terminal.
- Portas `8080` e `5432` livres.
- Conexão com a internet na primeira inicialização para baixar as imagens e dependências do build.

Não é necessário instalar Java, Maven ou PostgreSQL para iniciar a aplicação pelo Docker Compose.

## Início rápido

Na raiz do projeto, execute:

```powershell
docker compose up
```

O Compose constrói a aplicação pelo `Dockerfile`, inicia o PostgreSQL, aguarda o banco ficar pronto e inicia a aplicação. O Flyway aplica as migrações automaticamente.

Acesse:

- Aplicação: <http://localhost:8080/login>

Na configuração local padrão, a conta inicial de administrador é:

- E-mail: `admin@condominio.local`
- Senha: `admin123`

Essas credenciais e o segredo JWT padrão são apenas para desenvolvimento local. Altere-os antes de expor o sistema em qualquer ambiente compartilhado ou público.

### Personalizar configurações locais

O arquivo `.env` é opcional. Para substituir os valores padrão, copie o exemplo e edite as variáveis:

```powershell
Copy-Item .env.example .env
```

O arquivo `.env` é ignorado pelo Git. Não o inclua no repositório nem compartilhe credenciais reais.

| Variável | Finalidade | Padrão local |
|---|---|---|
| `DB_USER` | Usuário do PostgreSQL | `postgres` |
| `DB_PASSWORD` | Senha do PostgreSQL | `postgres` |
| `DB_NAME` | Banco da aplicação | `gerenciador_chamados` |
| `APP_TIMEZONE` | Fuso horário da aplicação e do banco | `America/Sao_Paulo` |
| `TOKEN` | Segredo usado para assinar e validar tokens JWT | `change-me` |
| `APP_BOOTSTRAP_ADMIN_ENABLED` | Cria a conta administrativa inicial | `true` |
| `APP_BOOTSTRAP_ADMIN_NOME` | Nome do administrador inicial | `Administrador` |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | E-mail do administrador inicial | `admin@condominio.local` |
| `APP_BOOTSTRAP_ADMIN_SENHA` | Senha do administrador inicial | `admin123` |
| `APP_CHAMADO_ATRASO_SCHEDULER_ENABLED` | Ativa a rotina de chamados em atraso | `true` |
| `APP_CHAMADO_ATRASO_SCHEDULER_INITIAL_DELAY_MS` | Atraso inicial da rotina, em milissegundos | `30000` |
| `APP_CHAMADO_ATRASO_SCHEDULER_FIXED_DELAY_MS` | Intervalo entre verificações, em milissegundos | `60000` |

### Encerrar a aplicação

Pressione `Ctrl+C` no terminal em que o Compose está executando. Para encerrar os serviços mantendo os dados do banco:

```powershell
docker compose down
```

Os dados do PostgreSQL ficam no volume `postgres_data` e são preservados por `docker compose down`. Para apagar também o banco local e seus dados, use `docker compose down -v`; essa operação é irreversível.

## Testes e cobertura

Para executar a suíte completa localmente, instale JDK 21 e Maven e mantenha o Docker Desktop em execução, pois os testes de integração usam Testcontainers com PostgreSQL:

```powershell
mvn verify
```

O JaCoCo verifica no `verify` a cobertura mínima de 40% de linhas nas classes incluídas para áreas comuns e reservas. A especificação registra o escopo medido e os resultados da validação.

## Banco de dados

As migrações Flyway ficam em `src/main/resources/db/migration`. Elas são aplicadas automaticamente na inicialização da aplicação; o Hibernate valida o esquema, sem criá-lo ou substituí-lo. A versão PostgreSQL usada pelo Compose é 16.

O diagrama relacional está em [`diagrama-relacional.drawio.svg`](diagrama-relacional.drawio.svg). As decisões de implementação, interpretações do enunciado, limitações e resultados de validação estão em [`ESPECIFICACAO.md`](ESPECIFICACAO.md).

## Estrutura geral do código

- `domain`: modelos e regras centrais do domínio.
- `application`: contratos dos casos de uso e paginação.
- `infrastructure/controller`: controllers web, formulários e configuração das telas.
- `infrastructure/service`: serviços que coordenam regras e persistência.
- `infrastructure/repository`: acesso a dados com Spring Data JPA.
- `src/main/webapp/WEB-INF/jsp`: páginas JSP organizadas por perfil.
- `src/test/java/.../unit`: testes unitários.
- `src/test/java/.../integration`: testes de integração web e de repositório.

## Observações

- Chamados e reservas usam regras de acesso diferentes. O morador consulta somente seus chamados e reservas; o administrador tem visão administrativa; o colaborador atua dentro do escopo configurado.
- Solicitações pendentes não bloqueiam a disponibilidade. A aprovação revalida conflitos da mesma área, e a implementação serializa aprovações concorrentes no PostgreSQL.
- Intervalos adjacentes são permitidos: se uma reserva termina quando outra começa, não há sobreposição.
- Anexos têm limite configurado de 5 MB por arquivo.
