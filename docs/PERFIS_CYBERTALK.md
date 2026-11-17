# Perfis de quem publica no CyberTalk

Nome e avatar (inicial) das publicações levam a `/comunidade/perfis/:id`.
A página oferece retorno ao CyberTalk e lista as publicações daquela conta,
em páginas de dez, da mais recente para a mais antiga (desempate por ID).
Os cartões e suas operações reaproveitam `PostTalk`: só a autora pode editar
ou excluir suas publicações. Não há edição de perfil nesta página.

## Visibilidade preservada

O perfil existente em `/perfil` é privado. A foto é explicitamente apresentada
como visível somente à titular. Não há configuração de compartilhamento dos
campos privados no modelo atual. Por isso, a consulta comunitária retorna
**somente ID e nome**, identidade que já aparece no feed; não lê a resposta do
perfil privado nem suas tabelas de foto, experiências ou certificações.
Foto real, apresentação, formação, competências, e-mail, currículo, credenciais,
indicadores privados e certificados não são expostos por esta funcionalidade.
O ícone genérico e a inicial não representam uma foto cadastrada.

Foi acordado planejar uma opção explícita de compartilhamento pela titular
em uma etapa futura. Nada foi tornado público automaticamente; campos vazios
não recebem informações inventadas. Nenhuma migração foi necessária.

## Contratos

- `autoraId` foi adicionado às respostas de publicações, preservando os demais
  campos e operações do feed. Nomes não são usados como identificadores.
- `GET /api/comunidade/perfis/{id}` retorna `{id,nome}` apenas para uma conta
  que tenha publicações. Conta inexistente ou sem publicações retorna 404.
- `GET /api/comunidade/perfis/{id}/publicacoes?pagina=0&tamanho=10` retorna
  `{itens,pagina,tamanho,total}`; página começa em zero, tamanho aceita 1–50.
  Valores inválidos retornam 400; páginas além do fim retornam lista vazia.
- Ambos exigem sessão válida e adesão à comunidade no backend (401/403), com
  respostas `Cache-Control: no-store`. Não existem operações de escrita nesse
  perfil ou endpoint de foto comunitária.
- No modelo atual, todas as publicações do feed são visíveis a participantes.
  A lista por autora utiliza a mesma projeção de consulta e a mesma exigência
  de participação. Se forem criadas novas regras de visibilidade de posts,
  ambas as consultas deverão ser atualizadas juntas.

## Validação realizada

- 19 testes backend aprovados em `cyberladies_auth_test`, incluindo cinco novos
  testes de perfil de autora e regressões do feed e perfil privado.
- Verificam duas contas com o mesmo nome, projeção mínima mesmo com dados e foto
  privados preenchidos, paginação/ordenação, sessão, adesão, 404, parâmetros,
  recusa de edição de perfil/post alheio e preservação da edição própria privada.
- 8 testes unitários frontend, lint, build e empacotamento backend aprovados.
- 14 cenários Chrome aprovados: 13 regressões de acessibilidade com APIs
  interceptadas e um percurso completo com duas contas reais sintéticas no
  banco separado `cyberladies_frontend_e2e`, API 18081 e Vite 5175. O percurso
  de perfis foi repetido após o ajuste de foco e espaçamento.
- No percurso real: login, adesão explícita, nome/avatar por teclado, onze
  publicações em duas páginas, retorno ao feed, ausência de campos privados,
  perfil inexistente, proibição de edição alheia e celular com contraste e texto
  a 200%. Capturas desktop/mobile foram inspecionadas.

Comandos em `frontend`: `npm run lint`, `npm run build`, `npm test` e, com a API
de teste ligada exclusivamente ao banco isolado e `E2E_ISOLATED=true`,
`npm run test:e2e -- tests/e2e/perfis.spec.js tests/e2e/acessibilidade.spec.js`.
Os cenários de perfis criam contas e posts somente no banco de teste e nunca
devem apontar para o ambiente de apresentação.

Limites: não há compartilhamento de foto/apresentação/competências, amizades,
sugestões ou diretório de contas. Uma conta sem publicações não tem perfil no
Talk; excluir sua última publicação remove essa possibilidade de consulta.
O navegador testado foi Chrome; não houve auditoria completa com leitor de tela.

O site continua em http://127.0.0.1:5178, usando o banco existente
`cyberladies_apresentacao` na porta 55439. Os testes não inseriram dados nele.
