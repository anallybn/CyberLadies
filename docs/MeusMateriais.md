# Meus Materiais — interface e leitura

Rota: http://localhost:5175/educadora/meus-materiais

## Escopo

Interface baseada no Figma: título/subtítulo, indicadores discretos, pesquisa, abas de status,
seletor de trilha, grade 3/2/1 e botão Novo Material. Reutiliza cabeçalho e popup da Educadora.
Sem novas imagens, upload, edição, persistência de rascunhos, publicação ou arquivamento.

Total e Publicados contam todas as aulas consultadas. Rascunhos mostra Em breve, sem número presumido.
As abas de rascunhos e arquivados explicam que esses recursos ainda não existem.
Os cards não têm botão de reprodução: Visualizar aula abre a rota funcional do Academy.

## Segurança verificada antes da integração

GET /api/educadora/dashboard chama AcessoConta.educadora() e exige sessão Bearer válida.
DashboardEducadoraService usa conta.id() nas consultas; DashboardEducadoraRepository consulta
academy_autoria WHERE educadora_id=?.
Nenhum ID de Educadora é aceito pelo serviço frontend.

GET /api/academy/trilhas/{id}/aulas é público no backend atual. Retorna metadados de aulas
do catálogo público. AcademyDados.Aula oculta complementar, videoProvedor e videoId com JsonIgnore.
Não é usado o endpoint de detalhe de aula para montar a listagem.

Fluxo:
1. Perfil confirmado pelo ContaProvider/GET /api/auth/conta.
2. Consulta privada do dashboard para obter as trilhas próprias, incluindo IDs.
3. Consulta de metadados públicos dessas trilhas, com no máximo quatro requisições simultâneas.
4. Exibição das aulas públicas, sem uso da lista limitada a cinco materiais recentes do dashboard.

O filtro local serve à apresentação e não é tratado como autorização de dados privados.
A consulta privada do servidor determina o vínculo de autoria; dados complementares consultados são públicos.
Não se deve reutilizar este fluxo para rascunhos privados: eles exigirão um endpoint privado próprio.
Educadora pendente pode consultar; nenhuma permissão de publicação foi adicionada.
CyberLadie e Empresa recebem estado de acesso exclusivo; backend mantém suas autorizações existentes.

Troca de conta/rota aborta consultas e descarta resultados antigos.
Não há fallback para catálogo global caso a consulta privada falhe.
Uma falha em qualquer trilha não mostra coleção parcial nem contadores zero.
Uma diferença entre a contagem privada e as aulas carregadas solicita nova tentativa.

## Limites

Não existe data de atualização nos DTOs atuais. A página informa organização por trilha e ordem das aulas;
não inventa datas nem anuncia ordenação por atualização.
Não há thumbnails, rascunhos ou estado arquivado no modelo atual.
A futura API privada de materiais deverá fornecer paginação, atualização e estados editoriais.
O serviço deve ser revisto antes de tornar rascunhos ou aulas privadas parte do modelo.

No banco isolado verificado nesta entrega, academy_autoria não tinha vínculos.
Portanto, a navegação real apresenta estado vazio para Educadoras sem trilhas atribuídas.
Nenhum vínculo ou material foi inserido para preencher a tela.

## Testes

Passaram build Vite e lint.
verify-meus-materiais.mjs executa apenas leituras HTTP/SQL; não cadastra conta, sessão, aula ou autoria.
Consulta contas existentes para testar a interface dos três perfis.
Usa títulos e metadados de aulas públicas já existentes no banco isolado.
Para testar a grade preenchida, controla somente no navegador a resposta de conta/autoria.
Isso é teste de componentes/estado, não autenticação real ou nova atribuição de autoria no servidor.

Verificações:
- Estado vazio com zero publicados e Rascunhos Em breve.
- Pesquisa por título e filtro por trilha sobre aulas existentes.
- Estados explicativos para Rascunhos e Arquivados.
- Grade 1/2/3 em 390/768/1440, sem overflow.
- Menu lilás e item Meus Materiais ativo; três opções futuras desabilitadas.
- Erro 503, falha de conexão, carregamento atrasado e recuperação.
- Nenhum material antigo exibido após troca para outra Educadora.
- Recusa visual para CyberLadie/Empresa e preservação dos menus desses perfis.
- Link Novo Material para a página existente.
- Rejeição real 401 pelo servidor de tentativa com ID de Educadora e token inválido.
- Ausência de requisições de escrita e erros JavaScript no Console.

A autorização por autoria foi confirmada no código do backend existente.
Como o ambiente não possui vínculos reais em academy_autoria, não foi executado um teste positivo
de duas contas reais com materiais próprios diferentes; essa cobertura depende de conteúdo autorizado
e vinculado ou de um ambiente de testes com fixtures explicitamente permitidas.

Comandos:
```powershell
docker exec cyberladies-auth-fase1-site npm run build
docker exec cyberladies-auth-fase1-site npm run lint
node frontend/scripts/verify-meus-materiais.mjs
node frontend/scripts/verify-educadora-visual.mjs
```

## Arquivos

Criados:
- frontend/src/pages/MeusMateriais.jsx
- frontend/src/pages/MeusMateriais.css
- frontend/src/services/educadoraMateriaisService.js
- frontend/scripts/verify-meus-materiais.mjs
- docs/MeusMateriais.md

Ajustados:
- frontend/src/App.jsx
- frontend/src/components/TopoEducadora.jsx
- frontend/src/components/MenuPopup.jsx
- frontend/scripts/verify-educadora-visual.mjs
- frontend/scripts/verify-dashboard-educadora.mjs
- frontend/scripts/verify-navegacao-perfis.mjs

Os scripts existentes foram ajustados para o novo link e o total de três opções futuras desabilitadas.
Nenhum CSS global ou de outra página foi alterado.
Sem alterações no backend, schema, autenticação, permissões ou banco; sem migrações.
Ambiente temporário: banco cyberladies_auth_restore_5958ab52de, site 5175.
Sem commit, push ou deploy.
