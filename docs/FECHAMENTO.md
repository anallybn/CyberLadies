# Fechamento da integração

Branch: `integracao/telas-apresentacao`. Checkpoint da retomada: `315460c`.
Main conferida e incorporada: `4dfaba7`, incluindo Chat e refinamentos visuais
da colega. Não houve substituição integral de um backend/frontend pelo outro.

## Resultado

- Acesso claro pelo topo e pelo menu às quatro telas existentes da educadora
  aprovada. Papéis vêm de `/auth/me`; aprovação vem de `/auth/conta`.
- Rotas exigem EDUCADORA e aprovação, aceitando o conjunto USUARIA + EDUCADORA.
  Pendentes e rejeitadas mantêm Minha solicitação e consulta do status/motivo.
  Usuária sem EDUCADORA e ADMIN não recebem acesso ao painel de educadora.
- Backend mantém a autorização, inclusive recusa de 403 nos acessos indevidos.
  Aprovação não concede publicação editorial, que continua indisponível.
- As páginas e estilos de dashboard, Enviar Material e Chat são iguais à main
  atual. Os 21 arquivos dos sete módulos legados permanecem idênticos à main.
- A documentação útil passou a `docs`, preservando conteúdo e atualizando links.
  Comandos operacionais permanecem em `scripts`. Pacotes Java não foram movidos.
- O script temporário de organização em target foi inspecionado, não repetido e
  não versionado. Configurações pessoais, arquivos gerados, acessos e dados estão
  fora dos commits.

## Validação executada

- 124 testes backend aprovados, sem falhas, erros ou ignorados, incluindo os
  11 testes do Chat da origem. A suíte concorrente de Chat usa banco automatizado
  próprio para não deixar fixtures nas contagens das demais suítes.
- 8 testes unitários frontend aprovados; lint e build de produção aprovados.
- 9 cenários Playwright/Chrome aprovados: login, ADMIN, CyberTalk, aprovada,
  pendente, rejeitada, usuária sem EDUCADORA, rotas por URL, múltiplos papéis,
  consultas e respostas das APIs, percurso de estudo/perfil/vagas e Chat salvo.
- Empacotamento do backend aprovado e links locais da documentação conferidos.
- Backup do banco fixo recuperado numa cópia de ensaio automatizado; V10 aplicada
  nessa cópia, comparando dados e sequências preexistentes. Só depois foi iniciada
  a versão integrada no banco fixo. Armazenamento não foi movido; as 5 contas e
  os 7 vínculos de papéis permaneceram.

## Diferenças já acordadas contra a main

O cadastro separado e o CRUD sem autorização de educadoras continuam substituídos
pelo fluxo pessoal de solicitação/análise ADMIN. Auth mantém papéis acumuláveis;
inicialização do banco usa Flyway em vez dos scripts avulsos da origem. Dashboard
e rascunhos de pendentes/rejeitadas permanecem bloqueados. CORS direto não é um
fluxo suportado: o frontend usa o proxy `/api`. Essas diferenças são explícitas
em [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md).

## Operação

Site: http://127.0.0.1:5178. Banco fixo: `cyberladies_apresentacao` em 55439.

```powershell
.\scripts\Iniciar-Apresentacao.ps1
.\scripts\Parar-Apresentacao.ps1
```

Backups, configuração persistente e instruções privadas de acesso continuam em
`%LOCALAPPDATA%\CyberLadies\apresentacao`, fora do Git. Não houve redefinição de
credenciais nesta etapa. Nenhum force push ou reaproveitamento de credenciais
do Git para chamadas de API é permitido.
