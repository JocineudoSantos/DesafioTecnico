# Especificação e decisões técnicas

## Escopo e comportamento preservado

As alterações desta etapa corrigem a organização e a configuração dos testes. Não alteram regras de negócio nem o SQL de produção. Os testes continuam exercitando os comportamentos já descritos em seus casos e as asserções existentes foram preservadas, exceto pelo preenchimento do dado que faltava em um fixture.

## Perguntas antes de iniciar

- **Solicitações pendentes ocupam o horário?** Interpretei que não: apenas reservas `APROVADA` bloqueiam a disponibilidade. O efeito é permitir pedidos simultâneos, deixando a validação definitiva para a aprovação administrativa.
- **Intervalos adjacentes conflitam?** Interpretei os períodos como semiabertos `[início, fim)`: se uma reserva termina no instante em que outra começa, elas não se sobrepõem.
- **Quais limites de capacidade, duração, expediente e antecedência se aplicam?** O enunciado não define esses valores; por isso, não criei limites de negócio além de exigir início futuro e fim posterior ao início.
- **O que ocorre com reservas após a desativação de uma área?** Elas permanecem no histórico e seguem o ciclo normal; a área deixa de aceitar novas solicitações.
- **Qual fuso deve orientar a interface?** A aplicação usa `APP_TIMEZONE`, com padrão `America/Sao_Paulo`; os instantes são persistidos com fuso e comparados pelo `Clock` configurado.

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
- **Continuidade:** a moderação administrativa e as interfaces de lista/calendário foram registradas nas etapas seguintes. O cancelamento será documentado em etapa própria.

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
- **Fora desta parte:** telas administrativas, calendário, cancelamento, notificações e integrações externas. A consulta administrativa foi implementada na terceira etapa e a agenda/calendário na quarta; o cancelamento permanece para etapa própria.
- **Com mais tempo:** acrescentaria auditoria imutável de todas as transições e testes de carga com muitas decisões para medir contenção por área.

### Validação desta parte

- Foram incluídos testes unitários para aprovação, conflito, negativa com motivo e transições inválidas, além de um teste de integração em PostgreSQL para aprovações conflitantes simultâneas.
- `mvn verify`: 78 testes executados, sem falhas, erros ou testes ignorados. O PostgreSQL 16 confirmou que, em duas aprovações simultâneas conflitantes, somente uma é aprovada. Outro teste aplicou as 20 migrações Flyway em um PostgreSQL vazio. A verificação JaCoCo da funcionalidade registrou 79,9% de cobertura de linhas e passou o mínimo exigido de 40%.

## Áreas comuns e reservas — terceira etapa: moderação administrativa na web

### Comportamento implementado

- A tela `/admin/reservas` lista as reservas, identifica solicitações pendentes e mostra área, período, morador e situação.
- Para solicitações `SOLICITADA`, o administrador pode aprovar ou enviar uma justificativa para negar. Para os demais estados, os formulários de decisão não são exibidos.
- Na lista, os botões **Aprovar** e **Negar** ficam lado a lado; o campo de justificativa aparece somente ao iniciar a negativa e continua obrigatório.
- A tela também apresenta a justificativa e os dados do responsável e horário da decisão quando disponíveis.
- As rotas web ficam restritas ao perfil `ADMINISTRADOR`; as regras de conflito, estado e justificativa permanecem no serviço da etapa anterior.

### Decisões técnicas e limites

- A consulta é exibida em tabela nesta etapa. A agenda/calendário para administrador e morador foi implementada na quarta etapa, conforme o RF-05 do desafio.
- O motivo é escapado pelo JSP ao ser exibido e o envio usa proteção CSRF do projeto.
- **Validação:** os 11 testes web da funcionalidade passaram, incluindo a restrição por perfil, os dados da fila, o envio das decisões e a resposta a uma justificativa vazia. `mvn verify` executou 84 testes, sem falhas, erros ou ignorados. JaCoCo registrou 83,1% de cobertura de linhas nas classes incluídas para áreas e reservas, acima dos 40% exigidos.

## Áreas comuns e reservas — quarta etapa: agenda mensal

### Comportamento implementado

- Administradores consultam em calendário mensal as solicitações pendentes e reservas aprovadas de todas as áreas, com identificação do morador.
- Moradores consultam no calendário somente as próprias solicitações pendentes e reservas aprovadas; a tabela de histórico continua disponível.
- A navegação permite avançar e voltar meses. O calendário usa o fuso configurado pela aplicação e destaca o dia atual.

### Decisões e limites

- A visualização mensal foi escolhida para atender à consulta por calendário sem adicionar dependência externa de calendário JavaScript.
- Solicitações negadas permanecem no histórico/lista, mas não são exibidas na agenda, pois não representam períodos reservados. Solicitações pendentes são identificadas e não bloqueiam a disponibilidade, conforme as regras definidas na primeira etapa.
- A grade reserva seis semanas e considera eventos que cruzam os limites do mês; um evento aparece em cada dia cujo intervalo se sobrepõe à reserva.
- **Validação:** `mvn verify` concluiu com 86 testes, sem falhas, erros ou ignorados. Os novos testes web verificam os eventos do mês, a exclusão de solicitações negadas da agenda e o escopo dos dados do morador. O JaCoCo aprovou o limite mínimo de 40% para as classes incluídas de áreas e reservas. Os testes de repositório e migrações usaram PostgreSQL 16 pelo Testcontainers.

## Áreas comuns e reservas — quinta etapa: regras de cancelamento no backend

### Comportamento implementado

- O morador proprietário ou um administrador pode cancelar reservas `SOLICITADA` ou `APROVADA` somente antes do horário inicial.
- A ação muda o estado para `CANCELADA`, preserva os dados da aprovação anterior e registra quando e por quem ocorreu o cancelamento.
- Reservas canceladas deixam de bloquear a disponibilidade porque as consultas de conflito consideram somente o estado `APROVADA`.
- Colaboradores, outros moradores, estados terminais e reservas cujo início chegou não podem ser cancelados por este fluxo.

### Decisões e limites

- A autorização e a transição de estado são verificadas no serviço, além de depender da identidade autenticada; a reserva é bloqueada durante a operação para serializar cancelamentos concorrentes com decisões administrativas.
- A migração V21 adiciona metadados próprios de cancelamento. O histórico anterior de decisões administrativas permanece nos campos da decisão, separado da auditoria do cancelamento.
- **Validação final:** os testes unitários e a suíte completa passaram; a V21 foi aplicada em PostgreSQL 16. Os números e o escopo medido estão registrados em “Resultado da validação”.

## Áreas comuns e reservas — sexta etapa: cancelamento nas telas

### Comportamento implementado

- Administradores podem cancelar reservas solicitadas ou aprovadas antes do início, pela lista administrativa.
- Moradores podem cancelar somente as próprias reservas solicitadas ou aprovadas antes do início, no histórico pessoal.
- As telas pedem confirmação antes do envio e exibem no histórico o horário e o responsável pelo cancelamento.
- A validação definitiva de permissão, situação e horário continua no serviço, para impedir ações inválidas mesmo se a requisição for enviada manualmente.

### Decisões e limites

- O botão só aparece para estados e períodos elegíveis; o serviço volta a verificar as regras e registra o ator autenticado.
- **Fora desta parte:** controles de cancelamento diretamente na agenda mensal; a ação está disponível nas listas/históricos.
- **Validação final:** as rotas web de cancelamento e as regras correspondentes passaram nos testes; ver “Resultado da validação”.

## Áreas comuns e reservas — sétima etapa: documentação do esquema

### Registro atualizado

- O diagrama relacional agora representa os campos de decisão (`decidida_em`, `decidida_por_usuario_id`, `motivo_negacao`) e de cancelamento (`cancelada_em`, `cancelada_por_usuario_id`) da tabela `reservas`.
- Os campos de usuário responsável são chaves estrangeiras para `usuarios`. A V21 permite dados de auditoria de cancelamento ausentes em registros cancelados antigos e exige que data e responsável sejam preenchidos juntos quando a auditoria existir.
- A tabela `reservas` também relaciona cada reserva à área comum e ao morador, guarda o intervalo, o estado e a data de criação. Os estados persistidos incluem `SOLICITADA`, `APROVADA`, `NEGADA` e `CANCELADA`.

### Limites

- O diagrama documenta a estrutura persistida; as permissões e regras para transições de estado continuam descritas nas etapas anteriores e implementadas no serviço.
- **Validação:** conferi a correspondência entre os campos do diagrama, as entidades e as migrações V19, V20 e V21. A suíte completa aplicou as 21 migrações em PostgreSQL 16 sem erro.

## Uso de inteligência artificial

- **Ferramentas usadas:** OpenAI Codex e um agente delegado, para inspecionar e implementar alterações em código, testes e documentação, investigar falhas e executar comandos de compilação e teste.
- **Sugestões aceitas da IA:** usar Testcontainers com PostgreSQL em vez de H2; simular `JwtService` sem substituir o filtro; e modelar áreas/reservas em nova migração Flyway com validação de regras no serviço.
- **Sugestões modificadas ou rejeitadas:** a primeira tentativa de simular o próprio filtro JWT foi revertida quando os testes mostraram que as requisições eram interrompidas antes dos controllers. A possibilidade de adaptar a consulta para H2 foi rejeitada porque o desafio e o projeto usam PostgreSQL e o SQL é específico desse banco.
- **Validação do conteúdo e do código:** conferi o enunciado, as decisões documentadas, as migrações e o modelo relacional. A validação final executou 50 testes unitários e a suíte completa de 91 testes com JDK 21. Os testes de migração e repositório usaram PostgreSQL 16 por Testcontainers; o Flyway aplicou as 21 migrações em banco vazio. JaCoCo mediu 89,33% de cobertura de linhas nas 16 classes de áreas e reservas incluídas no escopo unitário (318 de 356 linhas) e aprovou o mínimo de 40%.
- **Decisões não delegadas à IA:** o banco alvo PostgreSQL, a preservação do SQL e das regras de negócio, o escopo desta correção e a decisão de não tratar uma suíte parcial como aprovada foram definidos a partir do desafio e das orientações do usuário. A IA auxiliou na implementação e verificação, não definiu critérios de negócio ou aceite.
- **Interações relevantes (resumidas e sem dados sensíveis):**
  1. Foi relatado que o cadastro com e-mail já existente terminava em uma página 404. A IA investigou o tratamento web da exceção, corrigiu o retorno à listagem com mensagem e adicionou um teste de regressão.
  2. Foi esclarecido que o projeto deve usar PostgreSQL, não H2. A IA adaptou a validação com Testcontainers; os testes de consulta e migração foram executados em PostgreSQL 16.
  3. Foi relatada a exibição corrompida do separador em comentários e anexos. A IA substituiu os caracteres literais pela entidade HTML `&bull;` nas telas afetadas; a alteração foi conferida no build.
  4. Foi solicitado validar a etapa final. A primeira execução revelou fixtures sem relógio fixo e uma expectativa desatualizada para 20 migrações; a IA corrigiu os testes, adicionou casos de cancelamento e repetiu a suíte completa.

## Execução local

- Copie `.env.example` para `.env` e configure os valores locais, incluindo as variáveis `APP_BOOTSTRAP_ADMIN_EMAIL` e `APP_BOOTSTRAP_ADMIN_SENHA` para a conta administrativa inicial. O arquivo `.env` é ignorado pelo Git; não publique credenciais nele.
- Na raiz do projeto, execute `docker compose up --build` para iniciar PostgreSQL, aplicar as migrações Flyway e subir a aplicação. Acesse `http://localhost:8080/login` e use o e-mail e a senha definidos nas variáveis acima.
- Para executar os testes no computador, use JDK 21, Maven instalado e Docker disponível para os testes Testcontainers. `mvn verify` executa a suíte completa.
## Resultado da validação

`mvn verify` concluiu com **91 testes executados, 0 falhas, 0 erros e 0 ignorados**. Em execução separada dos testes unitários, passaram **50 testes**; o JaCoCo mediu **89,33% (318/356 linhas)** nas 16 classes de áreas e reservas configuradas no escopo e aprovou o mínimo de 40%. A execução de migrações aplicou as **21 versões**, incluindo V21, em PostgreSQL 16 pelo Testcontainers. O comando reproduzível para medir somente os testes unitários no PowerShell é:

```powershell
$unitTests = Get-ChildItem 'src/test/java/br/com/dunnastecnologia/chamados/unit' -Recurse -Filter '*Test.java' | ForEach-Object { $_.BaseName }
mvn "-Dtest=$($unitTests -join ',')" verify
```

A suíte completa roda com `mvn verify`. As duas execuções precisam de Docker disponível para os testes Testcontainers. A revisão do enunciado e a configuração do Compose não revelaram requisito funcional conhecido de áreas e reservas sem implementação; a configuração Compose passou, mas não foi feita uma nova subida dos containers nesta validação.
