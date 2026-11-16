> Histórico da origem. Nesta integração, identidade, migrações e alguns contratos foram reconciliados. Use [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md) para as regras e comandos atuais; não execute os SQLs/perfis antigos contra o banco da demonstração.

# Autenticação dos três perfis — Fase 1

## Estado da entrega e ativação

Implementação preparada e validada em cópia isolada do PostgreSQL.
A migração do banco principal NÃO foi executada. Sua execução depende de confirmação
expressa após revisão do backup, ensaio e riscos descritos abaixo.

Não reinicie a API principal com esta versão antes de aplicar a migração autorizada:
SchemaAuth verifica a estrutura e encerra a inicialização se ela estiver pendente.
Não executa migração automaticamente. O schema.sql do bootstrap não foi alterado
nesta fase. A API principal já iniciada permanece com sua versão anterior.

A instância de validação usa http://localhost:5175 e banco cyberladies_auth_restore_*.
O ambiente habitual continua em http://localhost:5173. Cadastros novos de Educadora
e Empresa só estarão ativos no habitual depois da migração e reinicialização aprovadas.

## Telas preservadas

Nenhum CSS foi modificado. PerfilPopup.jsx, Cadastro.jsx e cadastro/perfis.js foram
preservados byte a byte, incluindo os campos, validações visuais, animações e layout.
Login.jsx teve somente lógica de reconhecimento/direcionamento ajustada.
CadastroConcluido mantém seu layout e textos; os destinos ainda inexistentes de
Educadora/Empresa agora encaminham ao login correspondente.

Após login, CyberLadie mantém os destinos anteriores e o retorno para Perfil,
vaga ou estudo. Educadora vai ao catálogo Academy; Empresa vai ao catálogo de vagas.
Não há painéis de publicação nesta entrega. O menu reconhece o perfil e indica
Educadora pendente, sem alterar o componente visual.

O cache de perfil em sessionStorage serve somente à apresentação. A API não usa
perfil de URL, cache, IDs ou status enviados pelo cliente para conceder autorização.

## Identidade única e dados

Mantém talk_usuaria como estrutura física da conta existente, sem renomear IDs ou
criar outro sistema de autenticação:
- tipo VARCHAR(12): CYBERLADIE, EDUCADORA ou EMPRESA; default CYBERLADIE.
- Índice único em lower(btrim(email)), compartilhado pelos três tipos.
- auth_educadora: usuaria_id PK/FK, especialidade, instituicao e status.
- auth_empresa: usuaria_id PK/FK, empresa_id único/FK, CNPJ único e area_atuacao.
- cyber_empresa preserva seus registros e IDs. Cada cadastro empresarial novo cria
  sua identificação pública e associação à conta na mesma transação.

Contas anteriores recebem CYBERLADIE pela coluna default sem trocar IDs, senha_hash,
email, competencias, membro ou demais informações. talk_sessao e suas referências
não são migradas ou reemitidas.

A conta específica e a conta central são gravadas em uma transação. Falha no CNPJ,
duplicidade ou qualquer insert reverte o cadastro inteiro. Lacunas nas sequências
após rollback são normais e não significam registros incompletos.

Não associa automaticamente uma nova conta a uma empresa antiga apenas pelo nome.
A vinculação de registros empresariais administrativos anteriores exige processo
próprio de comprovação/atribuição, fora desta fase.

## Segurança

- Preserva o PBKDF2 existente e o Bearer opaco aleatório de 32 bytes.
- Somente o hash SHA-256 do token fica no banco, com validade de oito horas.
- Logout revoga somente o token apresentado e limpa token/perfil/status do cache.
- GET /auth/conta resolve identidade e tipo reais pelo banco a cada consulta.
- Cadastro aceita apenas os campos previstos. Campos extras de tipo, aprovação,
  permissões ou IDs privilegiados são rejeitados.
- Tipo é fixado pelo endpoint do backend. Trigger bloqueia mudanças do tipo da conta.
- Educadora começa PENDENTE; não há endpoint para autoaprovação, alteração de tipo
  ou concessão de privilégios. A aprovação futura ainda precisa de fluxo autorizado.
- Empresa não ganha publicação de vagas. O bloqueio de escrita de VagaController continua.
- AcessoTalk mantém o contrato existente, mas exige CYBERLADIE antes das operações
  privadas de comunidade, Perfil, candidaturas, estudo, progresso e certificados.
- O catálogo público permanece público para todos os perfis.
- Conta com cadastro específico incompleto não recebe sessão no login.
- Respostas de login/conta usam no-store e não contêm senha/hash/token_hash.
- O token continua em sessionStorage. Cache de perfil adulterado não modifica permissões.

O módulo legado em memória de educadoras/empresas não foi convertido em painel.
Seu status e seus IDs não participam da nova autenticação e não aprovam as contas
persistentes. Os futuros painéis devem usar AcessoConta e os dados persistentes.

## Endpoints

| Método | Caminho | Contrato |
| --- | --- | --- |
| POST | /api/auth/cadastro/usuaria | nome, email, senha, competencias opcional; 201 |
| POST | /api/auth/cadastro/educadora | nome, email, senha, especialidade, instituicao; 201 |
| POST | /api/auth/cadastro/empresa | nome, email, senha, cnpj, areaAtuacao; 201 |
| POST | /api/auth/login | email e senha; retorna token, perfil e statusEducadora |
| GET | /api/auth/conta | Bearer; id, nome, email, perfil e statusEducadora próprios |
| DELETE | /api/auth/sessao | Bearer; encerra sessão, 204 |

Nome: até 120 caracteres não brancos; email válido até 254; senha 8–128;
competências até 500; especialidade/instituição/área até 160 não brancos.
CNPJ segue os 14 dígitos e verificadores do formulário atual, validados também no backend.

Erros: 400 dados/campos inválidos; 401 credenciais/sessão inválidas; 403 operação
de outro perfil; 409 email ou CNPJ duplicado. Campos privilegiados não alteram dados.

## Migração, backup e ensaio

Arquivo: backend/src/main/resources/db/auth-perfis-v1.sql.
É separado da inicialização padrão e pode ser aplicado transacionalmente com psql.

Pré-checagem realizada no principal:
- Duas contas; emails normalizados e nenhum grupo duplicado por lower(btrim(email)).
- Estruturas e FKs existentes compatíveis com a evolução aditiva.
- Não foram alteradas credenciais nem consultados dados pessoais para exibição.

## Riscos e procedimento de ativação no principal

Ainda NÃO autorizado nem executado.
1. Conferir novamente duplicidades e mudanças ocorridas desde o ensaio.
2. Produzir backup novo imediatamente antes da ativação, especialmente se houver
   novos cadastros/publicações. O backup acima não inclui alterações posteriores.
3. Confirmar recuperação e guardar o backup fora do Git.
4. Coordenar uma pausa curta da API para impedir escritas durante a transição.
5. Aplicar a migração explicitamente, com ON_ERROR_STOP e --single-transaction,
   limites de espera de lock e de duração definidos.
6. Reiniciar a API e validar login antigo, sessão anterior, três cadastros e autorização.
7. Liberar o uso normal depois das verificações.

ALTER TABLE precisa de bloqueio da tabela de contas. Pode esperar por transações
ativas e causar indisponibilidade breve. O índice de email falha se aparecer
duplicidade normalizada; não corrige ou apaga contas automaticamente.
Se a migração falhar na transação, ela é revertida. Não restaurar backup por cima
de um banco que recebeu novas escritas sem avaliar a perda dessas alterações.
Não existe script de downgrade destrutivo.

## Resultados executados nesta entrega

- Build do frontend: aprovado.
- Lint do frontend: aprovado.
- Suíte completa no banco restaurado: 39 testes, zero falhas, zero erros, zero ignorados.
  Inclui os nove novos testes de autenticação e todas as regressões anteriores,
  inclusive os quatro testes reais de concorrência do Academy.
- Chrome, com origem real localhost:5175, sem falsificar cabeçalhos: os três
  cadastros pelos popups/formulários existentes; login com perfil da URL adulterado;
  retorno e tipo corretos; Educadora PENDENTE; logout; adulteração do cache sem
  concessão de privilégios; três contas e sessão preservadas após restart da API.
- Formulários verificados em 390, 768 e 1440 px, sem overflow horizontal.
- Console: sem erros JavaScript no fluxo aprovado.
- Hashes de PerfilPopup.jsx, Cadastro.jsx, cadastro/perfis.js e de TODOS os CSS:
  idênticos aos anteriores à fase.
- Ensaio de migração: 20 tabelas originais, 13 sequências e sessão anterior preservadas.
- Consulta final ao principal: coluna tipo ainda ausente; migração principal não executada.

Pendências: ativação no principal após confirmação; login manual das contas reais
anteriores (as senhas não foram solicitadas); leitores de tela/dispositivos físicos
e simulação de falha de rede durante cadastro. Testes e fixtures continuam somente
no banco isolado. Não há painel de Educadora/Empresa ou aprovação automática.

## Inventário desta fase

Criados:
- backend/src/main/java/CyberLadies/auth/dto/ContaDados.java
- backend/src/main/java/CyberLadies/auth/repository/ContaRepository.java
- backend/src/main/java/CyberLadies/auth/service/AcessoConta.java
- backend/src/main/java/CyberLadies/auth/service/ContaService.java
- backend/src/main/java/CyberLadies/auth/service/Credenciais.java
- backend/src/main/java/CyberLadies/auth/service/SchemaAuth.java
- backend/src/main/resources/db/auth-perfis-v1.sql
- backend/src/main/resources/db/auth-validacao-guard.sql
- backend/src/main/resources/application-auth-validacao.properties
- backend/src/test/java/CyberLadies/talk/AuthPerfisIntegrationTest.java
- frontend/scripts/verify-auth-perfis.mjs
- scripts/Validar-MigracaoPerfis.ps1
- docs/AutenticacaoPerfis.md

Modificados:
- backend/src/main/java/CyberLadies/talk/ContaApi.java
- backend/src/main/java/CyberLadies/talk/AcessoTalk.java
- backend/src/main/java/CyberLadies/talk/CorsLocal.java
- backend/src/test/java/CyberLadies/talk/AcademyConcurrencyTest.java (guard de banco isolado)
- frontend/src/services/authService.js
- frontend/src/services/talkApi.js
- frontend/src/services/academyService.js
- frontend/src/services/perfilService.js
- frontend/src/services/vagasService.js
- frontend/src/pages/Login.jsx
- frontend/src/pages/CadastroConcluido.jsx
- frontend/src/pages/Vagas.jsx
- frontend/src/pages/Academy.jsx
- frontend/src/pages/Aula.jsx
- frontend/src/pages/Mentorias.jsx
- frontend/src/pages/Trilha.jsx
- frontend/src/components/MenuPopup.jsx
- frontend/src/components/AcademyIniciar.jsx
- README.md

As mudanças nas páginas de catálogo somente evitam carregar dados ou oferecer
ações de CyberLadie para os outros tipos; não alteram seus layouts.
Nenhum arquivo CSS, asset, formulário de cadastro, popup, schema.sql ou compose.yaml
foi alterado nesta fase. Nenhum commit, push ou deploy realizado.