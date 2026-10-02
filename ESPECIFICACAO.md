# Especificação e decisões técnicas

## Escopo e comportamento preservado

As alterações desta etapa corrigem a organização e a configuração dos testes. Não alteram regras de negócio nem o SQL de produção. Os testes continuam exercitando os comportamentos já descritos em seus casos e as asserções existentes foram preservadas, exceto pelo preenchimento do dado que faltava em um fixture.

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

## Cadastro de usuário com e-mail duplicado

- **Comportamento inesperado:** tentar cadastrar um e-mail já existente terminava em uma página 404, embora o serviço já rejeitasse a duplicidade com uma regra de negócio.
- **Causa:** o `WebExceptionHandler` não incluía o pacote do `UsuarioApiController`; por isso, a exceção não era convertida em redirecionamento com mensagem.
- **Correção:** ampliei o escopo do tratamento para incluir esse controller. A tentativa agora retorna à listagem e mostra a mensagem de e-mail já cadastrado. Acrescentei um teste de regressão.
- **Validação:** o teste de regressão passou e a suíte completa ficou com 57 testes aprovados.

## Codificação de símbolos nas telas

- **Problema encontrado:** o símbolo de fechar alertas era exibido como `Ã` no login e nas mensagens compartilhadas.
- **Causa e decisão:** o caractere literal nos JSPs era interpretado incorretamente. Substituí por `&times;`, entidade HTML que o navegador renderiza corretamente.
- **Arquivos afetados:** `login.jsp` e o fragmento compartilhado `alerts.jspf`, cobrindo avisos de erro e sucesso.

- **Problema adicional:** o separador entre autor e data do comentário, e entre tipo e tamanho do anexo, também aparecia corrompido nas telas de chamados.
- **Correção:** substituí os caracteres literais por `&bull;` nos detalhes de chamados do morador, colaborador e administrador.
- **Validação e limite:** reconstruí e iniciei o container da pasta `DesafioTecnico`; `/login?error=true` respondeu HTTP 200. Os separadores `&bull;` foram incluídos no build das telas de morador, colaborador e administrador. O Dockerfile ignorou os testes automatizados neste build, então a conferência visual dos detalhes deve ser feita no navegador.


## Áreas comuns e solicitação de reservas — primeira etapa

### Comportamento entregue

- Administradores podem listar áreas ativas e inativas, cadastrar, editar e desativar áreas comuns.
- Moradores ativos podem consultar uma área e um intervalo, ver se há reservas aprovadas conflitantes, solicitar o período e consultar somente seu próprio histórico. Toda solicitação criada nesta etapa tem estado `SOLICITADA`.
- A interface acrescenta entradas próprias para administrador e morador. As rotas exigem os respectivos perfis, e os serviços repetem a validação de perfil para que as regras não dependam apenas da interface web.

### Organização técnica e motivo

- **Casos de uso (`AreaComumUseCases`, `ReservaUseCases`):** declaram as operações expostas pelos serviços. Essa fronteira mantém controllers web dependentes das capacidades de aplicação, sem concentrar validação e persistência nas telas.
- **Modelos (`AreaComum`, `Reserva`, `ReservaStatus`):** representam os dados e estados persistidos da funcionalidade e suas relações com o morador e a área.
- **Repositórios:** concentram consultas para áreas ativas, reservas do morador e sobreposição de períodos. A verificação de sobreposição usa início anterior ao fim pesquisado e fim posterior ao início pesquisado, filtrando por área e estado aprovado.
- **Serviços (`AreaComumService`, `ReservaService`):** validam perfil, estado ativo da área e do morador, limites do cadastro e regras de intervalo; associam a reserva ao morador autenticado e salvam a solicitação como `SOLICITADA`.
- **Controllers e formulários web:** convertem a entrada da tela em parâmetros do caso de uso e formatam o retorno para JSP. A tela administrativa cuida do cadastro de áreas; a tela do morador reúne consulta de disponibilidade, pedido de reserva e histórico próprio.
- **Configuração de tempo:** `ReservaTimeConfig` fornece `Clock`; `APP_TIMEZONE` define o fuso de exibição e conversão, com padrão `America/Sao_Paulo`. Isso evita depender do relógio do host nos serviços e testes.
- **Persistência e diagrama:** a V19 cria `areas_comuns` e `reservas`, com chaves estrangeiras, restrições de intervalo/estado e índices para listagem por morador e busca por área, estado e período. Atualizei `diagrama-relacional.drawio.svg` para mostrar as novas relações.

### Regras, interpretações e alternativas

- O fim precisa ser posterior ao início; ao solicitar, o início precisa estar no futuro. Intervalos são semiabertos `[início, fim)`, portanto uma reserva que termina exatamente quando outra começa não conflita.
- Só `APROVADA` ocupa o período. A consulta e as solicitações `SOLICITADA` não bloqueiam a disponibilidade. Essa leitura segue o enunciado, que distingue pedidos pendentes de reservas aprovadas.
- A área tem nome obrigatório, descrição opcional e indicador ativo. Escolhi desativação lógica em vez de exclusão física para preservar o vínculo e o histórico das reservas.
- Usei tabelas relacionais e PostgreSQL com uma nova migração Flyway, em vez de criar esquema em tempo de execução pelo Hibernate ou alterar migrações anteriores. Isso mantém o histórico de schema e permite validar a implantação em banco limpo e existente.
- Usei `TIMESTAMP WITH TIME ZONE`/`Instant` para persistir instantes sem depender do fuso local do servidor; a conversão para a entrada e apresentação da interface ocorre no fuso configurado.
- O morador da solicitação vem da identidade autenticada, em vez de aceitar um identificador de morador enviado pelo formulário. Assim, o cliente não escolhe em nome de quem criar a reserva.
- Capacidade da área, expediente, duração máxima, antecedência mínima além de início futuro, cobrança e participação de visitantes ficaram de fora: o enunciado não define essas regras e incluí-las exigiria decisões de negócio adicionais.

### Limitações, riscos e validação

- A disponibilidade exibida é uma consulta naquele instante. Pode mudar antes de uma solicitação ou futura aprovação administrativa.
- A aprovação, negativa com justificativa, cancelamento e calendário administrativo não fazem parte desta etapa.
- Na primeira etapa, a consulta seguida do salvamento ainda não protegia contra duas aprovações concorrentes; essa limitação foi resolvida nesta segunda etapa com bloqueio transacional no PostgreSQL e teste concorrente.
- Foram adicionados testes unitários para as regras dos serviços, testes web para navegação/perfis e testes de repositório com PostgreSQL via Testcontainers. No `mvn verify`, a suíte teve 72 testes aprovados, sem falhas, erros ou ignorados. JaCoCo mediu 77,4% de cobertura de linhas nas classes incluídas para a funcionalidade, acima do mínimo de 40% definido no desafio.
- **Continuidade:** a moderação administrativa foi registrada na etapa seguinte. O cancelamento e a interface de lista/calendário serão documentados em etapas próprias.

## Áreas comuns e reservas — segunda etapa: decisões administrativas

### Comportamento implementado

- O administrador pode consultar as reservas para moderação; a interface dessa consulta será entregue na etapa seguinte.
- Somente reservas `SOLICITADA` podem ser aprovadas ou negadas. Decisões repetidas ou sobre estados finais são recusadas.
- A aprovação verifica novamente a sobreposição com reservas `APROVADA` da mesma área no momento da decisão. Intervalos adjacentes continuam permitidos.
- A negação exige motivo não vazio; espaços externos são removidos antes de persistir. O motivo não recebe limite adicional porque o enunciado não define um.
- Aprovação e negação registram o instante e o usuário responsável. Esses dados, junto ao estado e ao motivo quando houver, dão rastreabilidade à decisão.

### Decisões técnicas, alternativas e riscos

- **Concorrência:** cada aprovação bloqueia a solicitação e, em seguida, a linha da área no PostgreSQL. Decisões de solicitações diferentes para a mesma área são serializadas antes da consulta de conflito. Escolhi esse bloqueio pessimista em vez de depender apenas de uma consulta seguida de atualização, que permitiria corrida, e em vez de introduzir uma extensão PostgreSQL e uma restrição de exclusão para intervalos.
- **Limite da garantia:** a proteção depende das aprovações passarem pelo serviço da aplicação. Escritas diretas no banco que não usem o mesmo bloqueio podem contornar a regra. O bloqueio por área pode reduzir a concorrência quando muitas aprovações da mesma área ocorrem ao mesmo tempo.
- **Histórico:** uma nova migração Flyway adiciona instante, autor e justificativa da decisão; migrações já aplicadas permanecem inalteradas. O autor referencia `usuarios`, pois a identidade autenticada é a origem confiável do administrador.
- **Fora desta parte:** telas e calendário administrativo, cancelamento, notificações e integrações externas. A próxima parte de interface deverá expor a consulta administrativa e a agenda/calendário exigidos pelo desafio.
- **Com mais tempo:** acrescentaria auditoria imutável de todas as transições e testes de carga com muitas decisões para medir contenção por área.

### Validação desta parte

- Foram incluídos testes unitários para aprovação, conflito, negativa com motivo e transições inválidas, além de um teste de integração em PostgreSQL para aprovações conflitantes simultâneas.
- `mvn verify`: 78 testes executados, sem falhas, erros ou testes ignorados. O PostgreSQL 16 confirmou que, em duas aprovações simultâneas conflitantes, somente uma é aprovada. Outro teste aplicou as 20 migrações Flyway em um PostgreSQL vazio. A verificação JaCoCo da funcionalidade registrou 79,9% de cobertura de linhas e passou o mínimo exigido de 40%.

## Áreas comuns e reservas — terceira etapa: moderação administrativa na web

### Comportamento implementado

- A tela `/admin/reservas` lista as reservas, identifica solicitações pendentes e mostra área, período, morador e situação.
- Para solicitações `SOLICITADA`, o administrador pode aprovar ou enviar uma justificativa para negar. Para os demais estados, os formulários de decisão não são exibidos.
- A tela também apresenta a justificativa e os dados do responsável e horário da decisão quando disponíveis.
- As rotas web ficam restritas ao perfil `ADMINISTRADOR`; as regras de conflito, estado e justificativa permanecem no serviço da etapa anterior.

### Decisões técnicas e limites

- A consulta é exibida em tabela nesta etapa. A agenda/calendário para administrador e morador permanece para a próxima etapa, conforme o RF-05 do desafio.
- O motivo é escapado pelo JSP ao ser exibido e o envio usa proteção CSRF do projeto.
- **Validação:** os 11 testes web da funcionalidade passaram, incluindo a restrição por perfil, os dados da fila, o envio das decisões e a resposta a uma justificativa vazia. `mvn verify` executou 84 testes, sem falhas, erros ou ignorados. JaCoCo registrou 83,1% de cobertura de linhas nas classes incluídas para áreas e reservas, acima dos 40% exigidos.

## Uso de inteligência artificial

- **Ferramentas usadas:** OpenAI Codex e um agente delegado, para inspecionar e implementar alterações em código, testes e documentação, investigar falhas e executar comandos de compilação e teste.
- **Sugestões aceitas da IA:** usar Testcontainers com PostgreSQL em vez de H2; simular `JwtService` sem substituir o filtro; e modelar áreas/reservas em nova migração Flyway com validação de regras no serviço.
- **Sugestões modificadas ou rejeitadas:** a primeira tentativa de simular o próprio filtro JWT foi revertida quando os testes mostraram que as requisições eram interrompidas antes dos controllers. A possibilidade de adaptar a consulta para H2 foi rejeitada porque o desafio e o projeto usam PostgreSQL e o SQL é específico desse banco.
- **Validação do conteúdo e do código:** conferi as declarações de pacote e imports, revisei o SQL e a versão do PostgreSQL no Compose, compilei e executei a suíte completa com JDK 21 e Docker Desktop e subi a aplicação completa pelo Compose. Após a segunda etapa de áreas e reservas, `mvn verify` executou 78 testes sem falhas, erros ou ignorados; JaCoCo mediu 79,9% de cobertura de linhas nas classes incluídas para a funcionalidade e aprovou o mínimo de 40%. Os testes de concorrência e das 20 migrações usaram PostgreSQL 16 por Testcontainers.
- **Decisões não delegadas à IA:** o banco alvo PostgreSQL, a preservação do SQL e das regras de negócio, o escopo desta correção e a decisão de não tratar uma suíte parcial como aprovada foram definidos a partir do desafio e das orientações do usuário. A IA auxiliou na implementação e verificação, não definiu critérios de negócio ou aceite.

## Resultado da validação

A execução anterior à funcionalidade de áreas e reservas terminou com 57 testes aprovados. Após a terceira etapa, `mvn verify` terminou com **84 testes executados, 0 falhas, 0 erros e 0 ignorados**. Os testes de repositório e das migrações usaram PostgreSQL 16 pelo Testcontainers. A verificação JaCoCo das classes da funcionalidade registrou **83,1%** de cobertura de linhas, acima do mínimo de 40%.

A execução de `docker compose up --build -d` iniciou a aplicação e o PostgreSQL em containers. O Flyway validou 19 migrações e aplicou a V19; a aplicação iniciou sem erros. A rota `/login` respondeu HTTP 200; `/admin`, `/colaborador` e `/morador` redirecionaram para `/login` sem sessão. Para evitar publicar segredos locais, `.env` foi criado a partir de `.env.example` e adicionado ao `.gitignore`.
