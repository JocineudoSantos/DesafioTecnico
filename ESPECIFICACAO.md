# Especificação

## 24/09/2026 — Alinhamento dos pacotes dos testes

- **Problema encontrado:** os 15 arquivos de teste estavam organizados fisicamente em diretórios `unit` e `integration`, mas declaravam os pacotes das classes de produção correspondentes.
- **Investigação:** verifiquei se os testes dependiam de membros com acesso de pacote das classes de produção. Não encontrei dependências desse tipo; os membros utilizados são públicos.
- **Decisão:** alinhei o pacote declarado de cada arquivo à sua pasta e adicionei imports explícitos das classes de produção antes resolvidas implicitamente pelo pacote compartilhado.
- **Impacto:** nenhuma lógica ou asserção dos testes foi alterada. A suíte ainda precisa ser executada novamente após essa mudança.
