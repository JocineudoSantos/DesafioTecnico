# Especificação e decisões técnicas

## Escopo e comportamento preservado

As alterações desta etapa corrigem a organização e a configuração dos testes, além do uso do Maven Wrapper no Windows. Não alteram regras de negócio nem o SQL de produção. Os testes continuam exercitando os comportamentos já descritos em seus casos e as asserções existentes foram preservadas, exceto pelo preenchimento do dado que faltava em um fixture.

## Pacotes dos testes

- **Inconsistência encontrada:** 15 arquivos estavam nas pastas de teste `unit` e `integration`, mas declaravam o pacote das classes de produção correspondentes.
- **Interpretação:** a estrutura de diretórios representa a separação pretendida entre testes unitários e de integração; os pacotes Java devem refletir essa estrutura.
- **Alternativa considerada:** manter os pacotes de produção para evitar imports. Foi descartada porque mantinha uma divergência entre o pacote declarado e a localização física dos testes.
- **Decisão e justificativa:** alinhei os pacotes declarados aos diretórios e adicionei imports explícitos das classes de produção. A verificação de acesso não encontrou dependências dos testes em membros package-private; os membros usados são públicos.
- **Efeito e risco:** não houve alteração de lógica ou asserções. Se futuramente um teste precisar acessar membros package-private, a decisão de pacote deve ser reavaliada em vez de reduzir a visibilidade de produção sem justificativa.

## Banco usado no teste de repositório

- **Inconsistência encontrada:** o teste do repositório configurava H2, embora o projeto e o desafio usem PostgreSQL. A consulta `marcarChamadosAtrasados` usa `UPDATE ... FROM` e aritmética de intervalo do PostgreSQL.
- **Alternativas consideradas:** reescrever a consulta para tentar suportar H2 e PostgreSQL ou executar o teste no banco definido para o projeto. Escolhi a segunda opção para validar o SQL real e evitar manter uma solução de produção diferente apenas para acomodar outro banco.
- **Decisão e justificativa:** configurei PostgreSQL 16 por Testcontainers, a mesma versão indicada pelo `docker-compose.yml`. O container é temporário e isolado do banco normal da aplicação.
- **Configuração e validação:** o teste de repositório desativa Flyway e deixa o Hibernate criar e remover o esquema para isolar a consulta; ele requer Docker para iniciar o PostgreSQL do Testcontainers. As 18 migrações Flyway foram validadas separadamente ao iniciar a aplicação pelo Docker Compose.
- **Decisão:** mantive os nomes fixos `postgres_condominio` e `app_condominio` na configuração do projeto.
- **Fora do escopo:** tornar a consulta compatível com H2 e revisar ou reescrever o SQL de produção.
- **Com mais tempo:** acrescentaria testes de integração que executem as migrações Flyway em PostgreSQL temporário, além de cobrir mais consultas nativas.

## Testes web e segurança

- **Problema encontrado:** os contextos `@WebMvcTest` não iniciavam sem a dependência `JwtService` do filtro JWT. Uma tentativa de substituir o filtro inteiro por mock fazia o filtro não encaminhar as requisições aos controllers, produzindo respostas vazias.
- **Alternativas consideradas:** simular o filtro JWT ou simular somente sua dependência. Mantive o filtro real e substituí apenas o `JwtService` por mock, para que as requisições continuem pela cadeia de filtros.
- **Decisão e justificativa:** adicionei uma configuração de segurança exclusiva dos testes web, com CSRF desativado e requisições permitidas, enquanto os casos fornecem explicitamente a autenticação simulada necessária ao controller. Essa configuração isola os testes de apresentação e encaminhamento.
- **Risco e limitação:** esses testes não comprovam, por si só, as regras completas de autorização da configuração de segurança de produção. Essa validação deve ser feita separadamente com testes dedicados à cadeia real e às permissões por perfil.
- **Fixture corrigido:** um teste de morador criava `TipoChamado` sem `prazoHoras`; o mapeamento da tela inclui esse valor em um mapa que não aceita nulos. Preenchi o fixture com 24 horas. Isso corrige os dados do cenário de teste, sem alterar o comportamento de produção.
- **Fora do escopo:** mudar autenticação, autorização, regras de perfil ou fluxos de negócio da aplicação.
- **Com mais tempo:** criaria uma suíte específica para validar acesso permitido e negado por perfil usando a configuração de segurança real.

## Maven Wrapper

- **Problema encontrado:** faltava `.mvn/wrapper/maven-wrapper.properties`, necessário para o Maven Wrapper, e o script `mvnw.cmd` formava incorretamente o caminho do arquivo no Windows.
- **Decisão e justificativa:** adicionei a configuração do Maven 3.9.16 e corrigi a montagem do caminho no script Windows. A versão escolhida foi executada com o JDK 21 do projeto.
- **Limitação:** na primeira execução em outra máquina, o Wrapper pode precisar baixar o Maven da internet. Se o Maven já estiver instalado, `mvn test` continua sendo uma alternativa.
- **Validação:** `mvnw.cmd -v` iniciou corretamente com Maven 3.9.16 e JDK 21.

## Cadastro de usuário com e-mail duplicado

- **Comportamento inesperado:** tentar cadastrar um e-mail já existente terminava em uma página 404, embora o serviço já rejeitasse a duplicidade com uma regra de negócio.
- **Causa:** o `WebExceptionHandler` não incluía o pacote do `UsuarioApiController`; por isso, a exceção não era convertida em redirecionamento com mensagem.
- **Correção:** ampliei o escopo do tratamento para incluir esse controller. A tentativa agora retorna à listagem e mostra a mensagem de e-mail já cadastrado. Acrescentei um teste de regressão.
- **Validação:** o teste de regressão passou e a suíte completa ficou com 57 testes aprovados.


## Codificação do símbolo nos alertas

- **Problema encontrado:** o símbolo de fechar alertas era exibido como `Ã` no login e nas mensagens compartilhadas.
- **Causa e decisão:** o caractere literal nos JSPs era interpretado incorretamente. Substituí por `&times;`, entidade HTML que o navegador renderiza corretamente.
- **Arquivos afetados:** `login.jsp` e o fragmento compartilhado `alerts.jspf`, cobrindo avisos de erro e sucesso.
- **Validação e limite:** reconstruí e iniciei o container da pasta `DesafioTecnico`; `/login?error=true` respondeu HTTP 200 com a entidade correta. Os testes automatizados foram ignorados durante o build desta alteração.

## Uso de inteligência artificial

- **Ferramenta usada:** OpenAI Codex, para inspecionar arquivos, propor e aplicar alterações em testes e documentação, investigar falhas e executar comandos de compilação e teste.
- **Sugestões aceitas da IA:** usar Testcontainers com PostgreSQL em vez de H2; simular `JwtService` sem substituir o filtro; e restaurar a configuração do Maven Wrapper.
- **Sugestões modificadas ou rejeitadas:** a primeira tentativa de simular o próprio filtro JWT foi revertida quando os testes mostraram que as requisições eram interrompidas antes dos controllers. A possibilidade de adaptar a consulta para H2 foi rejeitada porque o desafio e o projeto usam PostgreSQL e o SQL é específico desse banco.
- **Validação do conteúdo e do código:** conferi as declarações de pacote e imports, revisei o SQL e a versão do PostgreSQL no Compose, compilei e executei a suíte completa com JDK 21 e Docker Desktop. Também iniciei o Maven Wrapper para confirmar sua configuração e subi a aplicação completa pelo Compose.
- **Decisões não delegadas à IA:** o banco alvo PostgreSQL, a preservação do SQL e das regras de negócio, o escopo desta correção e a decisão de não tratar uma suíte parcial como aprovada foram definidos a partir do desafio e das orientações do usuário. A IA auxiliou na implementação e verificação, não definiu critérios de negócio ou aceite.

## Resultado da validação

A execução completa de `mvnw.cmd test`, com JDK 21 e Docker Desktop ativo, terminou com **57 testes executados, 0 falhas, 0 erros e 0 ignorados**. O teste de repositório utilizou PostgreSQL 16 pelo Testcontainers.

A execução de `docker compose up --build -d` iniciou a aplicação e o PostgreSQL em containers. O Flyway aplicou as 18 migrações e a aplicação iniciou sem erros. A rota `/login` respondeu HTTP 200; `/admin`, `/colaborador` e `/morador` redirecionaram para `/login` sem sessão. Para evitar publicar segredos locais, `.env` foi criado a partir de `.env.example` e adicionado ao `.gitignore`.
