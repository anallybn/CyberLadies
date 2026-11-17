> Histórico da origem. Nesta integração, identidade, migrações e alguns contratos foram reconciliados. Use [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md) para as regras e comandos atuais; não execute os SQLs/perfis antigos contra o banco da demonstração.

# Minhas Alunas — Educadora

## Acesso e interface

URL isolada: http://localhost:5175/educadora/minhas-alunas

Entrar com uma Educadora e abrir **Minhas Alunas** no menu existente. Somente contas EDUCADORA/APROVADA consultam dados. Contas pendentes recebem uma explicação, sem indicadores ou inscrições. CyberLadie e Empresa não acessam os dados.

Não foram aprovadas contas nem atribuídas trilhas automaticamente. Atualmente não há vínculos em academy_autoria no banco isolado: uma Educadora aprovada sem vínculos verá o estado vazio. O fluxo preenchido foi validado por testes transacionais com rollback e por respostas controladas apenas no navegador, sem inserir conteúdos para demonstração.

Pesquisa por nome (sem distinguir maiúsculas), filtros de progresso e seleção de trilha quando houver mais de uma trilha própria. Cada linha representa uma inscrição; a mesma pessoa pode aparecer em várias trilhas. Ver progresso abre um diálogo com aulas, conclusões e respectivas datas, sem acessar o perfil privado.

Indicadores não são afetados pelos filtros:
- Total de alunas distintas: pessoas inscritas nas trilhas próprias.
- Inscrições concluídas: inscrições com todas as aulas concluídas e pelo menos uma aula.
- Inscrições em andamento: demais inscrições, incluindo progresso zero.
- Inscrições nas trilhas: total de inscrições próprias.

## API privada, sem migrações

- GET /api/educadora/alunas?busca=&status=TODAS&trilhaId=...
- GET /api/educadora/alunas/{alunaId}/trilhas/{trilhaId}/progresso

Status de filtro: TODAS, ANDAMENTO, CONCLUIDAS. Bearer opaco existente; identidade e aprovação obtidas no backend. Todas as consultas restringem a autoria à conta autenticada. Detalhes não autorizados retornam 404; perfil/status incompatíveis, 403; sessão inválida, 401. Respostas usam Cache-Control: no-store. Não retornam e-mail, telefone, perfil privado ou identificadores de vídeos.

Consultas somente leitura sobre academy_autoria, academy_trilha, academy_inscricao, academy_aula, academy_conclusao e talk_usuaria. Nenhuma tabela, trigger, migração ou regra de publicação foi alterada.

Ambiente conferido: cyberladies_auth_restore_5958ab52de; site cyberladies-auth-fase1-site em 5175; API cyberladies-auth-fase1-api. API isolada executada com SPRING_SQL_INIT_MODE=never, inclusive nos testes, para não reaplicar scripts SQL. Ambiente principal não migrado nem reiniciado.

## Arquivos desta etapa

Novos:
- backend/src/main/java/CyberLadies/academy/controller/AlunasController.java
- backend/src/main/java/CyberLadies/academy/dto/AlunasDados.java
- backend/src/main/java/CyberLadies/academy/repository/AlunasRepository.java
- backend/src/main/java/CyberLadies/academy/service/AlunasService.java
- backend/src/test/java/CyberLadies/talk/AlunasIntegrationTest.java
- frontend/src/pages/MinhasAlunas.jsx
- frontend/src/pages/MinhasAlunas.css
- frontend/src/services/alunasService.js
- frontend/scripts/verify-minhas-alunas.mjs
- docs/MinhasAlunas.md

Integração de rota/menu:
- frontend/src/App.jsx
- frontend/src/components/TopoEducadora.jsx
- frontend/src/components/MenuPopup.jsx

Expectativas de navegação atualizadas nos testes existentes:
- frontend/scripts/verify-educadora-visual.mjs
- frontend/scripts/verify-navegacao-perfis.mjs
- frontend/scripts/verify-dashboard-educadora.mjs
- frontend/scripts/verify-meus-materiais.mjs

## Validação realizada

55 testes de backend passaram: AlunasIntegrationTest (6), RascunhoIntegrationTest, RascunhoConcorrenciaTest, AcademyIntegrationTest, AuthPerfisIntegrationTest, CyberVagasIntegrationTest, DashboardEducadoraIntegrationTest, PerfilIntegrationTest, CyberladiesApplicationTests e EducadoraServiceTest. SQL init desativado; banco isolado confirmado antes da execução.

Os novos testes cobrem 0%, parcial e 100%, pessoa em várias trilhas, indicadores, busca/filtros, ausência de trilhas/inscrições, isolamento, detalhes, dados privados e bloqueio de pendentes/CyberLadie/Empresa. As novas fixtures são transacionais com rollback.

Frontend:
- npm run build: passou.
- npm run lint: passou.
- verify-minhas-alunas.mjs: passou em 390, 768 e 1440 px, sem transbordamento horizontal; filtros, diálogo, Escape, erros, loading, estados vazios, perfis bloqueados e menu. Cenários preenchidos usam respostas controladas com registros existentes apenas no navegador; autorização real é testada no backend.
- verify-educadora-visual.mjs: passou, incluindo menu, fechamento, rolagem e regressão de Enviar Material.
- verify-meus-materiais.mjs: passou, incluindo três perfis, estados, filtros e navegação.

Fingerprints de academy_aula, academy_trilha, academy_inscricao, academy_conclusao e academy_certificado permaneceram iguais à referência anterior, assim como as funções de proteção do Academy. Nenhuma escrita no banco principal, commit, push ou deploy.

## Limites

Sem aprovação administrativa, criação de trilhas ou atribuição de autoria nesta etapa. Sem acesso ao perfil privado das alunas. Listagem sem paginação por enquanto; avaliar paginação se o volume crescer.