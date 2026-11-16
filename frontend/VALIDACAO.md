# Sessão, educadora e análise administrativa no frontend

## Integração do CyberTalk de origin/main

Integração realizada em `integracao/cybertalk-layout`, a partir do checkpoint
`1ea31ea`, com `origin/main` em `cc60a6b`. O merge `81ef086` incorporou o
frontend e preservou integralmente o backend do checkpoint, deixando de incorporar
os módulos em memória da main. Não há evidência suficiente para afirmar aprovação
específica da perda dessas funcionalidades. A correção em
`correcao/preservar-modulos-main` recupera nove módulos sem alterar seus contratos
ou armazenamento; os conflitos de educadora permanecem para revisão prévia.
A autenticação atual e Flyway continuam preservados. Consulte
[a revisão e validação da correção](../backend/CORRECAO_INTEGRACAO.md).

O feed e a coluna lateral, os cartões, os estilos de publicação, edição e
comentários e o menu popup mantêm o desenho da colega. O menu consulta os papéis
da sessão compartilhada, oferece os caminhos de educadora e ADMIN e usa o logout
existente. No celular, os caminhos ficam disponíveis pelo avatar. Escape fecha
o menu, o foco permanece dentro dele e retorna ao avatar ao fechar.

A adesão ao Talk continua explícita. Os filtros por tipo de autora ficam
desabilitados com indicação “Em breve”, pois o contrato atual das publicações
não fornece essa classificação. CyberAcademy, CyberVagas, perfil e chat continuam
indisponíveis. Grupos, sugestões e eventos mostram os estados vazios do layout;
não foram acrescentados dados fictícios ou serviços para essas áreas.

Validação desta integração em 15/11/2026:

- Backend: 53 testes aprovados, sem falhas ou ignorados, com PostgreSQL novo em
  `127.0.0.1:55439`; inclui migrações, sessão, cadastro, papéis e educadoras.
- Frontend: 8 testes unitários aprovados; lint e build aprovados.
- Chrome/Playwright: 7 testes aprovados com API em `18081`, banco exclusivo
  `cyberladies_frontend_e2e` e Vite em `5175`. Cobrem cadastro, login, retorno ao
  post, adesão explícita, publicação com anexo, leitura do anexo, curtida,
  comentário, edição, persistência após recarga, exclusão e logout com token
  invalidado. Também cobrem solicitação de educadora, aprovação, rejeição,
  conflito, filtro/paginação administrativa, 403, sessão expirada e erro de rede.
- Capturas do Chrome em desktop e celular foram inspecionadas: feed, coluna
  lateral, menu popup e telas de educadora/admin. Build não substituiu essa
  conferência visual. Não houve validação em Firefox ou Safari.

Os testes usaram um cluster novo e separado. O banco atual e os dados da
demonstração em `55440` não foram usados pelos testes. As configurações pessoais
de `.vscode`, credenciais protegidas e arquivos da demonstração ficam locais,
fora dos commits. A demonstração continua disponível em `http://127.0.0.1:5176`.

O site mantém React/Vite, cores, circuitos, cartões e componentes existentes.
A identidade e o conjunto de papéis vêm de `GET /api/auth/me`; nenhum papel é
obtido de `perfil` ou de outro parâmetro da URL. USUARIA e EDUCADORA podem
coexistir. As proteções de tela não substituem a autorização do backend.

## Rotas de tela

- `/comunidade`: mantém o Talk e o retorno ao post compartilhado após login.
- `/educadora`: solicitação profissional da conta pessoal e consulta do status.
- `/cadastro/educadora`: orienta usar cadastro/login pessoal e depois solicitar,
  sem oferecer outro cadastro com credenciais separadas.
- `/admin/educadoras`: lista, filtro por status e paginação de solicitações.
- `/admin/educadoras/:id`: consulta e decisão de uma solicitação pelo ADMIN.

ADMIN é direcionado à área administrativa após login; contas pessoais continuam
na comunidade. O menu oferece o acesso à solicitação de educadora. O cadastro
de administrador não é público; o comando operacional continua documentado em
`backend/ADMINISTRADOR.md`. As telas de empresas e cursos não foram implementadas;
suas APIs em memória foram recuperadas da main nesta correção e não integram a sessão.

A solicitação rejeitada mostra o motivo e não oferece reenvio. Uma solicitação
pendente pode ter o status atualizado. ADMIN precisa informar o motivo para
rejeitar. Em conflito de decisão, a tela recarrega o resultado atual e remove as
ações de análise. Não há reanálise ou revogação.

## Sessão e erros

`services/api.js` concentra requisições, Bearer, respostas vazias, blobs e erros.
`sessaoService.js` guarda somente o token em sessionStorage e mantém a identidade
em memória, carregando/revalidando os papéis no servidor. Nunca persiste um
perfil escolhido pelo cliente. `useSessao` e `RotaProtegida` compartilham esse
estado nas telas. `talkApi` usa o mesmo cliente das novas áreas.

- 401 em requisição protegida limpa a sessão e leva ao login com aviso; o post
  compartilhado é preservado quando aplicável.
- 401 do formulário de login mostra erro de credenciais, sem apagar outra sessão.
- 403 mostra acesso negado, preservando a sessão válida.
- Falhas de rede permitem nova tentativa. Falha no logout não simula sucesso.
- Resposta de uma requisição antiga não apaga um token mais recente.
- O logout central encerra a sessão no servidor e limpa a identidade local.
- Cadastro com 201 sem corpo é aceito, conforme o contrato do backend.

## Verificações sem banco

Na pasta frontend, com dependências instaladas:

```powershell
npm test
npm run lint
npm run build
```

Os testes unitários usam o runner do Node e API simulada. Build e lint não são
validação visual.

## Navegador com API e PostgreSQL reais isolados

Nunca use o banco de desenvolvimento. Prepare uma instância PostgreSQL
descartável em `127.0.0.1:55439`, com o usuário `migration_test`, e crie um banco
**novo** chamado `cyberladies_frontend_e2e`. A instância deve permitir conexão
local do usuário de teste. As fixtures usam psql com esse destino fixo; em
Windows o executável esperado é `C:/Program Files/PostgreSQL/16/bin/psql.exe`.
Não reutilize esse nome em um banco com dados reais.

Na raiz do projeto, em um terminal exclusivo da API de teste:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55439/cyberladies_frontend_e2e'
$env:DATABASE_USER = 'migration_test'
$env:DATABASE_PASSWORD = 'CREDENCIAL_DA_INSTANCIA_DESCARTAVEL'
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=18081'
```

Espere a API inicializar e migrar esse banco. Em outro terminal, na pasta frontend:

```powershell
$env:E2E_ISOLATED = 'true'
# Chrome instalado. Para Edge, use: $env:E2E_BROWSER = 'msedge'
npm run test:e2e
```

Playwright inicia e encerra seu Vite em `127.0.0.1:5175`, com proxy fixo para
`127.0.0.1:18081`; não reutiliza um servidor Vite já aberto. Sem a opção explícita
`E2E_ISOLATED`, os testes de navegador são ignorados. O teste usa dados aleatórios
e cria ADMIN exclusivamente por fixture SQL no banco descartável, sem criar
endpoint ou mecanismo público para isso. Remove somente as fixtures que criou.

Os testes percorrem cadastro pessoal, orientação de educadora, login, adesão,
solicitação, análise, rejeição/aprovação, conflito, filtro, paginação, consulta
do resultado, logout e retorno ao post. Usam servidor e banco reais; os cenários
de falha de rede e uma resposta 403 são simulados no navegador para exercitar
os estados de erro. A expiração de sessão é provocada no banco isolado.

Há também verificação de acesso negado apesar de `perfil=ADMIN`, carregamento,
nova tentativa e ausência de overflow horizontal em viewport de 390px.
Capturas desktop/celular ficam em `test-results/`, ignorado pelo Git.

Ao terminar, encerre a API e a instância PostgreSQL de teste, sem parar os
serviços do ambiente atual. A regressão backend pode ser executada nos bancos
isolados próprios, conforme `backend/MIGRACOES.md`.

## Resultados anteriores à integração de layout

- Build de produção e lint aprovados.
- 8 testes unitários do frontend aprovados.
- 6 fluxos Playwright no Chrome com API/PostgreSQL isolados aprovados.
- 53 testes existentes do backend aprovados, sem falhas ou ignorados.
- Capturas desktop e celular inspecionadas visualmente.

Não houve teste em Safari/Firefox nem ensaio Docker nessa etapa. Esses resultados
são históricos; os resultados atuais e as limitações da recuperação do backend
estão em [CORRECAO_INTEGRACAO.md](../backend/CORRECAO_INTEGRACAO.md).
