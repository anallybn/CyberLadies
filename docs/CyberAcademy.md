> Histórico da origem. Nesta integração, identidade, migrações e alguns contratos foram reconciliados. Use [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md) para as regras e comandos atuais; não execute os SQLs/perfis antigos contra o banco da demonstração.

# CyberAcademy

## Implementação por fases

1. Schema aditivo, DTOs, Repository/Service/Controller e catálogo público.
2. Página principal: hero, indicadores reais, pesquisa, filtros, trilhas, videoaulas
   e até três mentoras com horários livres em destaque. Todas as mentoras podem ser
   consultadas na página de agendamento.
3. Inscrição, lista ordenada de aulas, player seguro, conclusão e progresso real.
4. Disponibilidades, reserva confirmada, lista própria e cancelamento.
5. Emissão automática e consulta privada de certificados, inclusive no Perfil.
6. Testes de integração, concorrência, Chrome, correções e documentação.

As etapas foram compiladas/verificadas incrementalmente. Os módulos legados
/api/cursos, /api/mentorias e /api/certificados ainda usam memória e possuem semânticas
incompletas para este fluxo; foram preservados, mas não alimentam o CyberAcademy.
Não foi criado outro sistema de usuárias nem substituída a sessão por JWT.

## Arquitetura e persistência

Backend: pacote CyberLadies.academy, com DTOs explícitos, repositório JdbcClient
e serviços separados para catálogo, estudo, mentorias, vídeos e certificados.
Reutiliza AcessoTalk e talk_usuaria. O frontend usa academyService.js, hooks e
componentes compartilhados AcademyUI/Iniciar/Mentoras. Estilos são locais.

Tabelas adicionadas por schema.sql, sem apagar dados:
- academy_trilha: título, descrição, categoria, nível, versão, origem, elegibilidade,
  congelamento e data de criação.
- academy_aula: trilha, ordem única, duração em minutos, descrição, texto complementar,
  provedor, identificador de vídeo e confirmação administrativa de autorização.
- academy_inscricao: PK composta usuária/trilha e data.
- academy_conclusao: PK usuária/aula e FKs compostas para inscrição e aula/trilha.
- academy_mentora: dados profissionais públicos aprovados e vínculo opcional à conta.
- academy_disponibilidade: mentora, início/fim com fuso e ordem temporal validada.
- academy_agendamento: usuária, horário e status CONFIRMADO/CANCELADO.
- academy_certificado: UUID, usuária/trilha únicos, nome/título/versão/carga/aulas
  preservados como registro da emissão, além da data.

Há índices por usuária, trilha e mentora, unicidade por inscrição, conclusão,
ordem e certificado; índice único parcial impede reserva ativa duplicada.
O volume PostgreSQL dados mantém a persistência entre reinicializações.

## Regras de aprendizagem e certificados

- Categorias: FRONTEND, BACKEND, DADOS, UX_DESIGN, SOFT_SKILLS.
- Níveis: INICIANTE, INTERMEDIARIO, AVANCADO.
- Busca de até 150 caracteres em título/descrição; nas aulas inclui texto complementar.
  Ignora acentos e maiúsculas. Categoria e nível combinam-se com a pesquisa.
- Indicadores independem dos filtros: trilhas (cada versão conta), aulas e categorias
  cadastradas. Todos os conteúdos deste escopo são gratuitos; catálogo vazio exibe
  traço na gratuidade, em vez de uma porcentagem sem base.
- Trilha sem aulas não aceita inscrição. POST repetido de inscrição retorna 409.
- PUT de conclusão é idempotente; não duplica o total nem aceita identidade do cliente.
- Progresso = parte inteira de 100 × aulas concluídas / aulas existentes.
- A retomada aponta para a primeira aula ainda não concluída, na ordem do banco.
- O cadastro congela título, descrição, categoria, nível, elegibilidade, versão e
  todo o conteúdo/ordem das aulas na primeira inscrição. Triggers impedem alteração,
  remoção ou acréscimo posterior, inclusive por SQL normal.
- Alterações de conteúdo precisam de nova trilha/versão. Versões anteriores continuam
  disponíveis; não há arquivamento de trilhas congeladas nesta etapa.
- Ao concluir todas as aulas de uma trilha elegível, um trigger emite o certificado.
  O frontend não controla emissão, carga, nome ou percentual.
- Certificado é único por usuária e versão/trilha; alterações do registro são bloqueadas.
  Nome e título são snapshots, preservados mesmo se o perfil mudar depois.
- Certificados são privados, consultáveis por UUID autenticado; não há consulta
  pública que exponha o nome da aluna.
- O Perfil exibe certificadosPlataforma separadamente de certificacoes autodeclaradas.
  Certificados do legado em memória não são importados ou confundidos com estes.

## Regras de mentorias e concorrência

- Dados públicos das mentoras são cadastrados administrativamente com autorização
  para publicação. Vínculo opcional a talk_usuaria não publica email, foto privada
  ou dados pessoais do Perfil.
- Disponibilidades devem começar no futuro e ter fim posterior ao início.
- Uma mentora não pode ter disponibilidades sobrepostas.
- A reserva é confirmada imediatamente, sem email, pagamento ou videochamada.
- Considera todo o intervalo [início, fim): horários adjacentes são permitidos.
- A mesma usuária não pode reservar horários sobrepostos, mesmo com mentoras diferentes.
- Reserva e cancelamento bloqueiam, nesta ordem, usuária, mentora e disponibilidade.
  O PostgreSQL repete as verificações em triggers. Requisições simultâneas não passam
  pela janela entre consulta e gravação.
- Cancelamento é permitido somente antes do início. Repetir cancelamento já feito
  retorna 204. Reservas canceladas permanecem no histórico e liberam o horário.
- Horários com histórico de reserva não podem ser modificados.
- Uma conta só consulta/cancela seus próprios agendamentos; ID alheio retorna 404.

## API REST

Prefixo /api/academy. Identificadores inválidos retornam 400; conteúdo inexistente
retorna 404. Sessão ausente/expirada retorna 401. Sem inscrição, conclusão/progresso
de trilha retorna 403. Conflitos de inscrição/horário retornam 409.
Consultas privadas usam Cache-Control: no-store.

| Acesso | Método | Caminho |
| --- | --- | --- |
| Público | GET | /catalogo?busca=&categoria=&nivel= |
| Público | GET | /trilhas |
| Público | GET | /trilhas/{id} |
| Público | GET | /trilhas/{id}/aulas |
| Público | GET | /aulas |
| Público | GET | /mentoras |
| Público | GET | /mentoras/{id}/horarios |
| Privado | GET | /aulas/{id} |
| Privado | POST | /trilhas/{id}/inscricoes |
| Privado | GET | /me/inscricoes |
| Privado | GET | /me/progresso |
| Privado | GET | /trilhas/{id}/progresso |
| Privado | PUT | /aulas/{id}/conclusao |
| Privado | POST | /agendamentos |
| Privado | GET | /me/agendamentos |
| Privado | DELETE | /agendamentos/{id} |
| Privado | GET | /me/certificados |
| Privado | GET | /me/certificados/{uuid} |

POST /agendamentos recebe JSON com disponibilidadeId. Inscrição/conclusão não
recebem usuariaId nem percentual; usam a identidade da sessão. As listagens públicas
não devolvem identificadores de vídeo nem o texto complementar.
Não há endpoints públicos de publicação, edição de catálogo ou emissão manual.

## Vídeos autorizados

Não são aceitos URL arbitrária, HTML, scripts ou outros provedores.
O administrador informa somente provedor e ID, com uso_autorizado=true após
confirmar efetivamente a autorização de uso. Formatos:
- YOUTUBE: ID com 11 letras/dígitos/hífen/underscore.
- VIMEO: ID numérico positivo de até 12 dígitos.

O backend gera exclusivamente:
- https://www.youtube-nocookie.com/embed/ID
- https://player.vimeo.com/video/ID

A camada de serviço valida novamente o provedor/ID; CHECKs no banco rejeitam
IDs inválidos e material sem autorização confirmada. O texto complementar é
renderizado como texto, nunca HTML. O player tem sandbox e só é carregado após
clique, informando a conexão com o provedor externo. Cada nova aula pede novo carregamento.

Use vídeos disponíveis para incorporação, com permissões de domínio configuradas pelo
responsável. Vimeo não listado que exija hash adicional não é suportado nesta etapa.
A autorização de uso continua sendo responsabilidade do cadastro administrativo.

Referências técnicas consultadas:
[YouTube](https://developers.google.com/youtube/player_parameters) e
[Vimeo](https://help.vimeo.com/hc/en-us/articles/12426259908881-How-to-embed-my-video).

## Limitações e pendências

- Reprodução real de YouTube/Vimeo e permissões do detentor devem ser verificadas
  manualmente com conteúdo autorizado. Nenhum vídeo foi inventado ou cadastrado no principal.
- PDF, verificação pública de certificados, pagamentos, emails, videochamadas,
  painel editorial e upload de mídia não são implementados.
- Na ausência de miniatura cadastrada, o card usa um marcador visual de vídeo;
  não há hospedagem ou cadastro de miniaturas nesta etapa.
- Conclusão é declarada pela aluna pelo botão; não mede tempo assistido ou aplica prova.
- Catálogo/listas ainda não possuem paginação. Novas versões coexistem com as antigas.
- Cada disponibilidade é um horário fechado; não há recorrência automática ou lista de espera.
- Erros reais de rede durante gravação, leitores de tela, dispositivos físicos e
  anexos do CyberTalk ainda precisam de verificação manual.
- Nenhum commit, push ou deploy foi executado.

## Arquivos desta entrega

Criados:
- backend/src/main/java/CyberLadies/academy/dto/AcademyDados.java
- backend/src/main/java/CyberLadies/academy/repository/AcademyRepository.java
- backend/src/main/java/CyberLadies/academy/service/AcademyService.java
- backend/src/main/java/CyberLadies/academy/service/EstudoService.java
- backend/src/main/java/CyberLadies/academy/service/MentoriaAcademyService.java
- backend/src/main/java/CyberLadies/academy/service/CertificadoAcademyService.java
- backend/src/main/java/CyberLadies/academy/service/VideoSeguro.java
- backend/src/main/java/CyberLadies/academy/controller/AcademyController.java
- backend/src/main/java/CyberLadies/academy/controller/EstudoController.java
- backend/src/main/java/CyberLadies/academy/controller/MentoriaAcademyController.java
- backend/src/main/java/CyberLadies/academy/controller/CertificadoAcademyController.java
- backend/src/test/java/CyberLadies/talk/AcademyIntegrationTest.java
- backend/src/test/java/CyberLadies/talk/AcademyConcurrencyTest.java
- frontend/src/services/academyService.js
- frontend/src/hooks/useAcademy.js
- frontend/src/components/AcademyUI.jsx
- frontend/src/components/AcademyIniciar.jsx
- frontend/src/components/AcademyMentoras.jsx
- frontend/src/pages/Academy.jsx e Academy.css
- frontend/src/pages/Trilha.jsx
- frontend/src/pages/Aula.jsx
- frontend/src/pages/Mentorias.jsx
- frontend/src/pages/CertificadoAcademy.jsx
- frontend/scripts/verify-academy.mjs
- docs/CyberAcademy.md

Modificados:
- backend/src/main/resources/schema.sql
- backend/src/main/java/CyberLadies/perfil/dto/PerfilDados.java
- backend/src/main/java/CyberLadies/perfil/service/PerfilService.java
- frontend/src/App.jsx
- frontend/src/components/secoes.js
- frontend/src/pages/Login.jsx
- frontend/src/pages/Perfil.jsx
- docs/Perfil.md