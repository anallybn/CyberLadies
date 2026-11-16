# Navegação e menus por perfil — integração frontend

## Resultado

O popup usa exclusivamente a conta confirmada por GET /api/auth/conta, com o Bearer opaco existente.
O tipo da URL e o cache de perfil do sessionStorage não escolhem o menu.
Nenhum CSS foi alterado nesta etapa.

- Educadora: mesmo menu lilás em Dashboard, Enviar Material, Academy, Talk e Vagas.
- CyberLadie: menu e acesso ao Perfil preservados.
- Empresa: menu anterior com identificação EMPRESA, sem herdar funções da Educadora ou da CyberLadie.
- Meus Materiais, Minhas Alunas, Chat e Perfil da Educadora permanecem desabilitados com Em breve.
- Dashboard, Enviar Material, CyberAcademy, CyberTalk e consulta de CyberVagas possuem links existentes.

## Consulta e sessão

ContaProvider compartilha estado de conta entre popup e controles de estudo/candidatura.
Revalida na entrada e ao navegar para outro caminho; F5 inicia nova consulta.
Cada resultado está associado ao token e ao caminho usados na consulta.
AbortController impede aproveitar respostas canceladas de uma navegação anterior.

Enquanto consulta, o popup é neutro: Menu da conta / Confirmando sua conta.
Em falhas de transporte, 503 ou respostas sem perfil válido, não escolhe um perfil alternativo,
não apaga a sessão e oferece Tentar novamente.
Somente 401 da consulta da conta limpa a sessão correspondente. Uma resposta antiga não limpa um token novo.
A sessão expirada apresenta Entrar; os redirecionamentos já existentes das páginas privadas são preservados.

O cache antigo de perfil é atualizado a partir da resposta confirmada apenas por compatibilidade.
O popup, as candidaturas de Vagas e os controles de estudante alterados nesta etapa leem o contexto confirmado.
Não foram trocados o login, a senha protegida, o token, o armazenamento de sessão ou o backend.

## Permissões

As regras do servidor permanecem como estavam:
- Educadora participa do Talk e pode estudar.
- Vagas pode ser consultado/pesquisado; candidatura exige CyberLadie.
- Publicação de vagas continua bloqueada no backend.
- Empresa não ganhou acesso ao Talk ou a operações de estudante.
- Verificações no frontend controlam apresentação; o servidor continua autorizando cada operação.

## Ambiente

Site: http://localhost:5175
API temporária: cyberladies-auth-fase1-api
Proxy: http://cyberladies-auth-fase1-api:8080
Banco: cyberladies_auth_restore_5958ab52de

Nenhum arquivo de backend, schema ou migração foi alterado.
O banco principal e a API principal não foram migrados ou reiniciados.
Os testes reais usam cadastros e sessões de teste no banco isolado por APIs existentes.
Uma publicação de validação no Talk foi criada e removida pelo teste.
Não houve commit, push ou deploy.

## Validação

Passaram:
- Build Vite e lint ESLint.
- Login real de CyberLadie, Educadora e Empresa, inclusive com perfil contrário na URL.
- Navegação e F5: Educadora nas cinco páginas; CyberLadie e Empresa em Talk, Academy e Vagas.
- Menu correto após adulterar o cache de perfil antes da navegação.
- Perfil da Empresa preservado inclusive ao receber recusa do Talk.
- Quatro opções futuras da Educadora desabilitadas.
- Publicação e exclusão de post de teste pela Educadora após adesão normal à comunidade.
- Pesquisa de vagas e bloqueios de candidatura/publicação para Educadora e Empresa.
- Carregamento deliberadamente atrasado com popup neutro.
- Falha 503 e falha de conexão sem apagar token válido; recuperação por Tentar novamente.
- Resposta 401 controlada e rejeição real de token revogado.
- Logout dos três perfis.
- Responsividade 390/768/1440 e ausência de erros JavaScript no Console.
- Regressão visual do dashboard/menu e Enviar Material, incluindo 390x568 e rolagem interna.

Comandos:
```powershell
docker exec cyberladies-auth-fase1-site npm run build
docker exec cyberladies-auth-fase1-site npm run lint
node frontend/scripts/verify-navegacao-perfis.mjs
node frontend/scripts/verify-educadora-visual.mjs
```

O primeiro script usa contas reais de teste no banco isolado e controla somente o transporte da consulta de conta
nos cenários de atraso/falha. O segundo é uma inspeção visual somente de leitura: fornece ao navegador snapshots
dos dados existentes, incluindo a conta, sem criar sessão no servidor; não substitui o teste real de autorização.

## Arquivos desta etapa

Novos:
- frontend/src/components/ContaProvider.jsx
- frontend/src/context/contaContext.js
- frontend/src/hooks/useConta.js
- frontend/scripts/verify-navegacao-perfis.mjs
- docs/NavegacaoPerfis.md

Ajustados:
- frontend/src/main.jsx
- frontend/src/services/authService.js
- frontend/src/components/MenuPopup.jsx
- frontend/src/components/TopoEducadora.jsx
- frontend/src/components/AcademyIniciar.jsx
- frontend/src/pages/Vagas.jsx
- frontend/src/pages/Academy.jsx
- frontend/src/pages/Trilha.jsx
- frontend/src/pages/Aula.jsx
- frontend/src/pages/Mentorias.jsx
- frontend/scripts/verify-educadora-visual.mjs

TopoLogado e os layouts das páginas não precisaram ser alterados: usam o mesmo MenuPopup, agora ligado à conta real.
