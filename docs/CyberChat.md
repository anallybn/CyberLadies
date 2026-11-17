# Integração atual

O CyberChat publicado na main foi preservado e reconciliado com a identidade
`conta`, papéis acumuláveis e aprovação atual. Use Flyway V10 e os comandos de
[AMBIENTE_LOCAL.md](AMBIENTE_LOCAL.md); o SQL avulso e os destinos específicos da
documentação de origem abaixo são históricos. Não executar esses scripts antigos
diretamente no banco fixo. A suíte concorrente usa `cyberladies_auth_chat_test`.

---

# CyberChat — mensagens privadas por contexto

## Acesso manual

Abra http://localhost:5175/chat ou Chat no menu da conta. O sistema usa o Bearer opaco existente e a resposta de GET /api/auth/conta. Educadora conserva seu cabeçalho e menu lilás; CyberLadie e Empresa usam o cabeçalho/menu existentes. O retorno seguro /chat após login foi adicionado sem trocar os destinos habituais.

1. Entre com sua conta no ambiente isolado.
2. Abra Chat e Nova conversa.
3. Escolha um vínculo elegível. A API deriva os dois participantes; não existe busca global de usuários.
4. Escreva e envie. Na conta da outra participante, confira o indicador de não lidas e abra a conversa.
5. Recarregue, saia e entre novamente para verificar a persistência.
6. Pesquise pelo nome ou título do contexto. Use a paginação da lista e Carregar mensagens anteriores quando disponíveis.
7. Em celular, o botão Voltar retorna à lista.

Sem vínculos elegíveis, a tela permanece vazia e explica os requisitos. Não foram aprovadas contas, atribuídas trilhas ou criadas conversas fictícias no isolado de uso manual para demonstrar a interface. É necessário usar vínculos reais já cadastrados e autorizados para testar envio manualmente.

## Permissões

| Contexto | Participantes e requisitos |
| --- | --- |
| TRILHA | CyberLadie inscrita e Educadora APROVADA com autoria explícita. |
| MENTORIA | CyberLadie agendada e Educadora APROVADA vinculada à mentora; agendamento CONFIRMADO para criar/enviar. |
| CANDIDATURA | CyberLadie candidata e conta Empresa vinculada à empresa responsável pela vaga; vaga ativa para criar/enviar. |

Educadora/Empresa, CyberLadie/CyberLadie e Educadora/Educadora não são habilitadas. Uma Educadora estudante não ganha acesso a pares fora deste escopo. Mentoras sem conta vinculada e trilhas sem autoria não geram vínculos de chat.

Sessão, participação e contexto são verificados em todas as operações. Conta pendente/rejeitada não vê conversas de alunas. Revogação da aprovação bloqueia seu acesso; a aluna conserva histórico autorizado, com novos envios bloqueados. Cancelamento de mentoria e fechamento de vaga preservam histórico autorizado, mas bloqueiam novos envios. Conclusão da trilha e passagem do horário de mentoria confirmada não encerram automaticamente o acompanhamento.

Manipulação de IDs não concede acesso. Recurso indisponível retorna 404; ação proibida, 403; sessão inválida, 401. Não são retornados e-mail, telefone, senha, token ou perfil privado. Nome de apresentação e tipo da outra conta são os dados de participante exibidos.

## Persistência e integridade

Migração independente: backend/src/main/resources/db/cyberchat-v1.sql.

Aplicada explicitamente somente no isolado confirmado, após pg_dump customizado e restauração de recuperação verificada. Nenhuma entrada foi adicionada ao schema.sql nem aos perfis de inicialização. API isolada permanece com SPRING_SQL_INIT_MODE=never. O script exige banco cyberladies_auth_* e deve ser aplicado uma única vez, transacionalmente, com ON_ERROR_STOP.

Novas tabelas:
- chat_conversa: UUID, contexto, contas derivadas, referências e sequência/atividade.
- chat_participante: exatamente as duas contas inseridas pelo trigger da conversa; terceiros e alterações são recusados.
- chat_mensagem: UUID, autora participante, texto, sequência por conversa, chave de envio e horário do servidor.
- chat_leitura: cursor monotônico de cada participante, com FK para mensagem da mesma conversa.

A view chat_contexto consulta vínculos existentes. Funções/triggers novos ficam limitados ao chat. Nenhuma tabela ou trigger do Academy foi alterado.

Contexto e participantes são imutáveis. Mensagens não podem ser editadas/excluídas nesta etapa. FKs preservam referências e histórico. Nenhuma exclusão em cascata de históricos foi adicionada.

O envio bloqueia a conversa, verifica novamente o vínculo e serializa sua sequência. Aprovação e contexto sujeitos a encerramento também são bloqueados para que alteração e envio concorrentes sejam ordenados. Chave de envio única por conversa/autora: retry com mesmo texto devolve a mesma mensagem; reutilização para outro texto retorna 409. A criação concorrente do mesmo contexto resulta numa única conversa.

Texto simples de até 2.000 caracteres, não vazio; controles inválidos são recusados. HTML é apresentado como texto pelo React. Limite inicial de 20 mensagens novas por minuto por conversa/autora. Retries idempotentes não consomem uma nova mensagem. Nenhum conteúdo de mensagem é registrado por código de logging do chat; erros de banco recebem resposta genérica.

## API

Todos os endpoints usam /api/chat, autenticação existente e Cache-Control: no-store.

| Método | Caminho | Contrato |
| --- | --- | --- |
| GET | /contextos?pagina=0 | Vínculos elegíveis, 30 por página. |
| POST | /conversas | {contexto: chave retornada por /contextos}; cria ou recupera conversa autorizada. |
| GET | /conversas?busca=&pagina=0 | Próprias conversas, 30 por página, atividade mais recente primeiro. |
| GET | /conversas/{id} | Resumo autorizado e disponibilidade de envio. |
| GET | /conversas/{id}/mensagens | Últimas 50 mensagens, devolvidas em ordem crescente. |
| GET | /conversas/{id}/mensagens?antes=sequencia | Página anterior por cursor. |
| GET | /conversas/{id}/mensagens?apos=sequencia | Novas mensagens por cursor. |
| POST | /conversas/{id}/mensagens | {texto, chaveEnvio: UUID}. |
| PUT | /conversas/{id}/leitura | {sequencia: número positivo de mensagem desta conversa}. |

Respostas paginadas usam itens e mais. antes e apos são mutuamente exclusivos. Pesquisa case-insensitive sobre participante e título do contexto nas próprias conversas; não pesquisa pessoas nem conteúdo globalmente.

Leitura nunca retrocede e só avança no frontend quando o fim do histórico está visível, com a aba visível. Mensagens próprias não contam como não lidas.

## Interface e atualização

Polling de cerca de cinco segundos, sem sobreposição, pausado em aba oculta. Requisições são canceladas ao trocar conversa/conta. Falha transitória preserva a sessão e o texto. Token inválido segue o tratamento de expiração existente.

Mensagem com resposta de envio perdida conserva texto e chave para retry. O envio usa atualização incremental do servidor, evitando saltar mensagens concorrentes. Troca de conta desmonta o estado anterior. O compositor bloqueia envio em contexto encerrado. A tela distingue erro de consulta, carregamento, pesquisa vazia, ausência de conversas e ausência de vínculos.

Desktop: lista lateral e histórico. Celular: uma área por vez. CSS restrito a cc-*, sem alteração de estilos globais, mascotes ou CSS das demais páginas.

## Arquivos

Criados:
- backend/src/main/java/CyberLadies/chat/controller/ChatController.java
- backend/src/main/java/CyberLadies/chat/controller/ChatErros.java
- backend/src/main/java/CyberLadies/chat/dto/ChatDados.java
- backend/src/main/java/CyberLadies/chat/repository/ChatRepository.java
- backend/src/main/java/CyberLadies/chat/service/ChatService.java
- backend/src/main/resources/db/cyberchat-v1.sql
- backend/src/test/java/CyberLadies/chat/ChatIntegrationTest.java
- frontend/src/pages/CyberChat.jsx
- frontend/src/pages/CyberChat.css
- frontend/src/components/chat/ChatConversa.jsx
- frontend/src/hooks/useChat.js
- frontend/src/services/chatService.js
- frontend/scripts/verify-chat.mjs
- docs/CyberChat.md

Integração:
- frontend/src/App.jsx
- frontend/src/components/MenuPopup.jsx
- frontend/src/components/TopoEducadora.jsx
- frontend/src/pages/Login.jsx

Expectativa do item Chat atualizada nos testes:
- frontend/scripts/verify-dashboard-educadora.mjs
- frontend/scripts/verify-educadora-visual.mjs
- frontend/scripts/verify-meus-materiais.mjs
- frontend/scripts/verify-minhas-alunas.mjs
- frontend/scripts/verify-navegacao-perfis.mjs

## Validação

11 testes novos de backend passaram na cópia restaurada de testes, com SQL automático desativado: três perfis, idempotência, paginação, não lidas, isolamento, campos privilegiados, pares proibidos, pendência/revogação, encerramento/cancelamento, concorrência de criação/envio, revogação concorrente, cancelamento concorrente, integridade de participantes e cursor.

A regressão anterior executou 55 testes: 54 passaram. O teste sessaoAnteriorAMigracaoContinuaReconhecida falhou porque a sessão sintética antiga já expirou: a API devolve corretamente 401. A sessão e a regra de expiração foram preservadas; o teste legado não foi modificado para mascarar essa condição.

Um teste legado sem rollback criou duas contas de fixture no isolado de uso manual. Elas foram identificadas por comparação com uma restauração do backup e removidas somente após confirmação de identidade sintética e ausência de vínculos. Ao final, as 24 tabelas anteriores têm os mesmos digests/contagens do backup. Funções do Academy também permanecem idênticas. Sequências podem avançar nos testes; não foram reiniciadas.

As fixtures novas do chat ficaram exclusivamente na cópia de testes. O teste Chrome usa a interface em 5175 com interceptação que encaminha /api exclusivamente à API da cópia de testes, com guarda de destino; não fabrica respostas de sucesso. Falhas de conexão/resposta perdida são induzidas apenas no transporte do navegador.

Responsividade verificada em 390, 768 e 1440 px. Regressões de Enviar Material, Meus Materiais, Minhas Alunas e menu passaram. Build Vite e lint ESLint passaram.

## Limites

Sem anexos, chamadas, edição/exclusão de mensagens, diretório de pessoas, notificações externas, presença ou confirmação de leitura da outra participante. Sem painel empresarial de vagas/candidaturas. Pontos de entrada contextuais nas páginas existentes podem ser acrescentados depois; Nova conversa já permite usar os vínculos autorizados sem ampliar os CRUDs existentes.

A lista de conversas/contextos usa paginação por página; atividade concorrente pode reordenar a lista entre consultas. O histórico usa cursor sequencial. Não há tempo real por WebSocket/SSE. Limitação de frequência inicial por conversa/autora; proteção global distribuída e ferramentas de denúncia/bloqueio ficam para evolução posterior.

Nenhuma operação no banco principal, commit, push, deploy ou alteração da Vercel nesta entrega.
