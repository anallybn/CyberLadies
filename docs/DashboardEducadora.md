# Dashboard da Educadora — ambiente isolado

Implementação desta etapa: dashboard privado, autoria explícita, menu e permissões compartilhadas.
Não inclui painel editorial, aprovação administrativa, publicação de aulas ou painel empresarial.

## Ambiente validado

- Site temporário: http://localhost:5175
- Dashboard: http://localhost:5175/educadora/dashboard
- API temporária: container `cyberladies-auth-fase1-api`.
- PostgreSQL: `cyberladies_auth_restore_5958ab52de`.
- Perfil Spring: `auth-validacao`.
- O site temporário usa `API_PROXY=http://cyberladies-auth-fase1-api:8080`.
- Principal (porta 5173, banco `cyberladies`) não foi migrado ou reiniciado.

A configuração e o banco foram consultados antes das migrações/testes.
A migração contém uma guarda que recusa bancos fora de `cyberladies_auth_*`.
Ela é carregada somente pelo perfil isolado, depois do schema existente e da migração de perfis.
Não aplicar no principal: qualquer futura promoção exige análise de compatibilidade, backup recuperável e autorização específica.

## Banco e publicação

`db/educadora-dashboard-v1.sql` acrescenta:
- `academy_autoria(trilha_id PK/FK, educadora_id FK auth_educadora, vinculado_em)`.
- Índice por Educadora e trigger que impede alteração/exclusão de autoria.
- `academy_disponibilidade.tema`, opcional, com até 160 caracteres.

Nenhum vínculo é criado automaticamente. As trilhas antigas permanecem sem Educadora.
Autoria é metadado separado para não modificar trilhas congeladas.
Os triggers de congelamento, aulas, inscrições, progresso, certificados e conflitos de horário não foram alterados.

O catálogo atual é integralmente público: não existe estado editorial de rascunho.
Por isso, aulas vinculadas são materiais publicados e o status exibido é Publicado.
Não foi criado um segundo sistema de publicação ou um campo de status sem efeito no catálogo.
A futura fase editorial deverá introduzir estados e visibilidade de modo consistente em todas as consultas.
Novas versões devem criar uma nova trilha com origem/versão e novo vínculo de autoria, preservando versões inscritas.

O vínculo é administrativo explícito e não tem endpoint público de escrita.
Não atribuir o catálogo legado em massa. Nenhuma associação foi executada nesta entrega.
A mentora precisa ter `academy_mentora.usuaria_id` vinculado à conta correta, sem associação automática por nome/e-mail.
Tema ausente aparece como “Tema não informado”.

## API e segurança

`GET /api/educadora/dashboard` exige Bearer opaco válido e tipo EDUCADORA do PostgreSQL.
Retorna `Cache-Control: no-store`. A consulta usa o ID autenticado, nunca um ID enviado pelo cliente.
O snapshot read-only REPEATABLE READ mantém os agregados consistentes entre si.

Resposta:
- nome, statusEducadora, podePublicar=false;
- indicadores: materiaisPublicados, alunasAlcancadas, conclusoes;
- trilhas: ID, título, inscrições;
- mentorias: até 5 confirmadas futuras, aluna, tema, início/fim;
- materiais: até 5 aulas recentes, título, trilha, total de aulas da trilha, status.

Definições:
- Materiais publicados = quantidade de aulas das trilhas explicitamente vinculadas.
- Alunas alcançadas = pessoas distintas inscritas nessas trilhas, sem duplicação entre trilhas.
- Conclusões = pares pessoa/trilha com todas as aulas concluídas; não depende de elegibilidade para certificado.
- Gráfico = inscrições por trilha, incluindo zero.
- Agenda = mentora vinculada à Educadora autenticada; exclui cancelamentos e horários já iniciados.
- Avaliação e visualizações não possuem implementação real; interface mostra Em breve.
- Horários usam o fuso do dispositivo, informado na interface.

Educadora PENDENTE pode ler seu dashboard e estudar.
Publicação continua proibida inclusive para APROVADA.
As rotas reservadas `/api/educadora/trilhas`, `/aulas`, `/publicacoes` recusam operações de escrita.
Rotas de estudo existentes mantêm apenas as operações específicas de inscrição, progresso, certificados e agendamento.
Não há API de aprovação, mudança de tipo ou autoria para a própria conta.

Permissões:
- CyberLadie: comportamento atual preservado.
- Educadora: dashboard próprio, estudante do Academy, participante do Talk, consulta de vagas.
- Educadora: sem candidatura, publicação empresarial ou acesso ao Perfil privado da CyberLadie.
- Empresa: sem acesso ao dashboard da Educadora e sem permissões compartilhadas de estudante/Talk.
Alterar sessionStorage só altera apresentação; não concede autorização no backend.

## Interface e navegação

Layout baseado na imagem aprovada: cabeçalho lavanda, menu/mascote/avatar, saudação,
cinco cards em 3+2, barras horizontais, agenda e tabela. CSS restrito ao dashboard.
Em celular a tabela permite rolagem interna, sem ampliar a largura da página.
Estados de carregamento, erro com nova tentativa, vazio, não autenticado e perfil sem acesso são tratados.

Login usa o perfil retornado pelo backend:
- EDUCADORA → /educadora/dashboard;
- CYBERLADIE e EMPRESA preservam seus destinos anteriores.

Menu apresenta Dashboard, CyberAcademy, CyberTalk e CyberVagas.
Minhas trilhas, Criar trilha, Gerenciar videoaulas, Minhas alunas, Gerenciar mentorias e Perfil profissional estão desabilitados com Em breve.
Não há links para páginas futuras inexistentes.

## Testar manualmente

1. Abra http://localhost:5175 (não use 5173 para esta etapa).
2. Use o popup e formulário existentes de Educadora para cadastrar uma conta, ou entre com uma conta EDUCADORA do banco isolado.
3. Após login, confira /educadora/dashboard, nome e status pendente.
4. Sem autoria vinculada: três indicadores zero, dois Em breve e estados vazios.
5. Abra menu pelo ícone ou avatar; acesse Academy, Talk e Vagas e retorne pelo item Dashboard.
6. No Talk, entre na comunidade para participar. No Academy, use os fluxos existentes de estudante.
7. Confirme que não é possível candidatar-se nem publicar conteúdos.
8. Teste 390, 768 e 1440 px; a tabela pode ser deslizada dentro do card no celular.
9. Entre com outra Educadora: dados da primeira não devem aparecer.
10. Sem sessão, a rota retorna ao login. Com CyberLadie/Empresa, mostra acesso exclusivo e a API retorna 403.
11. Use Sair e confirme que a sessão é revogada.

Os dados preenchidos de métricas e mentorias são exercitados por fixtures transacionais nos testes de integração;
essas fixtures não atribuem nem alteram trilhas reais. Não foram inseridas amostras permanentes para preencher o dashboard.
Os scripts de navegador cadastram contas identificadas como teste apenas no banco isolado; a publicação temporária de Talk é removida pelo próprio teste.
A suíte de concorrência preexistente pode manter suas próprias fixtures isoladas e congeladas; não as exclua por comandos genéricos.

## Validação automatizada

44 testes de backend passaram: contexto (1), serviço legado (4), concorrência Academy (4),
Academy (10), autenticação (9), CyberVagas (5), dashboard (5), Perfil (6).

Os testes novos cobrem agregação distinta, conclusão parcial/integral, autoria, duas Educadoras,
trilhas não atribuídas, agenda confirmada/cancelada, dados pessoais mínimos, status pendente/aprovado sem publicação,
estudo da Educadora com certificado e preservação da imutabilidade.

O teste de dashboard usa Chrome headless, verifica login por perfil real, menu, dados vazios, reload,
permissões mesmo com cache adulterado, Talk, estudo, consulta de vagas, logout e rota privada.
Capturas em 390/768/1440 são gravadas em %TEMP%/cyberladies-dashboard-{largura}.png.
O teste de perfis também reinicia somente a API temporária para verificar persistência.
O Vite no volume Windows precisou de reinício somente do container temporário para descartar módulos antigos.

## Arquivos desta etapa

Backend:
- src/main/java/CyberLadies/academy/controller/DashboardEducadoraController.java (novo)
- src/main/java/CyberLadies/academy/dto/DashboardEducadoraDados.java (novo)
- src/main/java/CyberLadies/academy/repository/DashboardEducadoraRepository.java (novo)
- src/main/java/CyberLadies/academy/service/DashboardEducadoraService.java (novo)
- src/main/java/CyberLadies/academy/service/AcademyService.java
- src/main/java/CyberLadies/academy/service/EstudoService.java
- src/main/java/CyberLadies/academy/service/CertificadoAcademyService.java
- src/main/java/CyberLadies/academy/service/MentoriaAcademyService.java
- src/main/java/CyberLadies/auth/service/AcessoConta.java
- src/main/java/CyberLadies/talk/AcessoTalk.java
- src/main/resources/db/educadora-dashboard-v1.sql (novo)
- src/main/resources/application-auth-validacao.properties
- src/test/java/CyberLadies/talk/DashboardEducadoraIntegrationTest.java (novo)
- src/test/java/CyberLadies/talk/AuthPerfisIntegrationTest.java

Frontend:
- src/pages/DashboardEducadora.jsx (novo)
- src/pages/DashboardEducadora.css (novo)
- src/App.jsx
- src/pages/Login.jsx
- src/components/MenuPopup.jsx
- src/components/AcademyIniciar.jsx
- src/services/authService.js
- src/pages/Academy.jsx
- src/pages/Trilha.jsx
- src/pages/Aula.jsx
- src/pages/Mentorias.jsx
- scripts/verify-dashboard-educadora.mjs (novo)
- scripts/verify-auth-perfis.mjs

Documentação:
- docs/DashboardEducadora.md (novo)

Mudanças anteriores de Vagas, Perfil, Academy e Fase 1 que já estavam sem commit foram preservadas.
Sem commit, push ou deploy.
