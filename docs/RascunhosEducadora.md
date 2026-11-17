> Histórico da origem. Nesta integração, identidade, migrações e alguns contratos foram reconciliados. Use [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md) para as regras e comandos atuais; não execute os SQLs/perfis antigos contra o banco da demonstração.

# Rascunhos reais da Educadora

## Acesso manual

Ambiente isolado: http://localhost:5175

1. Entre com uma Educadora (PENDENTE também é permitida).
2. Abra http://localhost:5175/educadora/enviar-material.
3. Informe um título. Os demais campos podem ficar incompletos; deixe o vídeo vazio se ainda não tiver um vídeo autorizado.
4. Clique Salvar rascunho. A confirmação vem do servidor, e a URL passa a incluir ?rascunho=<uuid>.
5. Abra http://localhost:5175/educadora/meus-materiais. Use Todos/Rascunhos, pesquisa e filtro por trilha.
6. Clique Continuar editando. Altere os campos e salve novamente.
7. Recarregue ou saia e entre novamente: o rascunho permanece no PostgreSQL.
8. Publicar continua desabilitado. Arquivamento, exclusão e publicação não foram implementados.

Ao sair com alterações locais, a página pede confirmação em links internos, Voltar do navegador e Sair.
F5/fechamento usam o aviso nativo quando o navegador permite; a mensagem nativa é controlada pelo navegador.
Uma saída confirmada pode descartar alterações que ainda não foram salvas.
Não há autosave nem cópia permanente do conteúdo privado no navegador.

## Endpoints privados

Base: /api/educadora/materiais/rascunhos

| Método | Caminho | Resultado |
|---|---|---|
| POST | base | Cria/reconhece repetição idempotente; 201 |
| GET | base | Resumos de todos os rascunhos próprios |
| GET | /{uuid} | Conteúdo do rascunho próprio |
| PUT | /{uuid} | Atualiza/reconhece repetição idempotente; 200 |

POST e PUT exigem Idempotency-Key com UUID.
PUT também exige revisao no JSON.
GETs e respostas de escrita usam Cache-Control: no-store.

AcessoConta.educadora() fornece a proprietária pela sessão opaca existente.
Não há parâmetro de proprietária. Todos os SELECT/UPDATE usam educadora_id e ID.
CyberLadie/Empresa recebem 403; sem sessão, 401; IDs alheios/inexistentes, 404.
Campos fora do contrato, incluindo status, permissões e educadoraId, são rejeitados.

DTO de escrita:
titulo, descricao, trilhaId, duracaoMinutos, tipo, tags, videoProvedor, videoId, usoAutorizado e revisao (somente atualização).

Limites:
- Título obrigatório, até 160 caracteres.
- Descrição até 5.000 caracteres.
- Duração opcional, inteiro positivo.
- Até oito tags de até 30 caracteres, aparadas e deduplicadas.
- YouTube: ID de 11 caracteres permitidos; Vimeo: numérico de até 12 dígitos.
- Nenhum HTML de incorporação ou URL arbitrária é aceito como identificador.
- Confirmação de uso pode ficar desmarcada no rascunho. Não concede publicação.

## Concorrência e falhas

Criação: UNIQUE(educadora_id,chave_criacao) e hash canônico calculado pelo servidor.
Repetição do mesmo pedido retorna o registro existente; chave com outro conteúdo retorna 409.

Atualização: bloqueio da linha própria e comparação da revisão.
Duas abas com a mesma revisão não sobrescrevem uma à outra.
Última operação/hash reconhecem resposta perdida sem avançar a revisão novamente.
Um retry antigo depois de outras edições retorna conflito; não desfaz alterações posteriores.

O frontend conserva a mesma operação/payload enquanto o resultado é incerto.
Se os campos forem alterados nesse intervalo, a confirmação explica que ainda existem alterações locais.
Conflitos preservam os campos e oferecem recarregar a versão salva, com confirmação.
Respostas de um editor já abandonado não redirecionam a usuária de volta para ele.

## Interface preservada

Nenhum CSS foi modificado.
Hashes de EnviarMaterial.css, MeusMateriais.css e MenuEducadora.css coincidem com o estado anterior.
Cabeçalho e menu receberam somente callback opcional para confirmar saída do editor.
O roteador BrowserRouter e a autenticação não foram substituídos.

Meus Materiais combina resumos privados com a consulta existente de aulas públicas próprias.
Indicadores, pesquisa e status usam dados completos carregados.
Rascunhos têm IDs separados por status e link Continuar editando.
Datas de atualização ordenam os rascunhos; aulas publicadas continuam por trilha/ordem, pois o DTO público não fornece atualização.
Uma falha em qualquer consulta não é apresentada como uma coleção completa ou zero materiais.

## Arquivos desta etapa

Novos:
- backend/src/main/resources/db/academy-rascunhos-v1.sql
- backend/src/main/java/CyberLadies/academy/dto/RascunhoDados.java
- backend/src/main/java/CyberLadies/academy/controller/RascunhoController.java
- backend/src/main/java/CyberLadies/academy/service/RascunhoService.java
- backend/src/main/java/CyberLadies/academy/repository/RascunhoRepository.java
- backend/src/test/java/CyberLadies/talk/RascunhoIntegrationTest.java
- backend/src/test/java/CyberLadies/talk/RascunhoConcorrenciaTest.java
- frontend/src/services/rascunhosService.js
- frontend/src/services/rascunhoSaida.js
- frontend/src/hooks/useRascunho.js
- frontend/src/hooks/useAlteracoesPendentes.js
- frontend/scripts/verify-rascunhos.mjs
- docs/RascunhosEducadora.md

Alterados:
- backend/src/main/resources/application-auth-validacao.properties
- frontend/src/pages/EnviarMaterial.jsx
- frontend/src/pages/MeusMateriais.jsx
- frontend/src/services/educadoraMateriaisService.js
- frontend/src/components/TopoEducadora.jsx
- frontend/src/components/MenuPopup.jsx
- frontend/scripts/verify-educadora-visual.mjs
- frontend/scripts/verify-meus-materiais.mjs

## Pendências deliberadas

Publicação, aprovação administrativa, arquivamento, exclusão e upload continuam fora do escopo.
Nenhum rascunho é promovido automaticamente a aula.
A migração do principal continua sem autorização.
Sem commit, push ou deploy.
