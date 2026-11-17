# Correção da integração: módulos da main

> Registro da correção anterior, preservada no checkpoint `15796f5`.
> A integração posterior das novas telas incorpora os contratos seguros e
> persistentes de vagas/candidaturas da main, conforme decisão recebida, e mantém
> os outros sete módulos em memória. O estado atual, validações e roteiro estão
> em [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md).

## Base e preservação do trabalho

Em 15/11/2026, o fetch confirmou `origin/main` em `cc60a6b` e a integração em
`81ef086`. A branch local `correcao/preservar-modulos-main` parte dessa integração.
Nenhum commit publicado foi reescrito. Não houve push nem merge nesta correção.
As alterações pessoais de `.vscode` foram preservadas e não fazem parte do diff.

Os módulos abaixo chegaram à main pelo merge `d242902`, mas não existiam no
checkpoint `1ea31ea`. O merge `81ef086` manteve o backend desse checkpoint e
deixou de incorporá-los. O registro anterior de “decisão confirmada” não comprova
aprovação específica da perda de cada funcionalidade e foi corrigido.

## Funcionalidades recuperadas

Os 27 arquivos de produção foram recuperados integralmente de `origin/main`,
sem alteração de métodos, rotas, DTOs, validações, respostas ou serviços.

| Módulo | Prefixo | Operações preservadas |
| --- | --- | --- |
| Agenda | `/api/agenda` | CRUD e POST `/verificar-termino` |
| Avaliações | `/api/avaliacoes` | CRUD e POST `/validar-nota` |
| Candidaturas | `/api/candidaturas` | CRUD e POST `/validar` |
| Certificados | `/api/certificados` | CRUD e POST `/validar` |
| Cursos | `/api/cursos` | CRUD e POST `/validar` |
| Empresas | `/api/empresas` | CRUD e POST `/validar` |
| Mentorias | `/api/mentorias` | CRUD e POST `/validar` |
| Notificações | `/api/notificacoes` | POST `/verificar-leitura` |
| Vagas | `/api/vagas` | CRUD e POST `/validar` |

CRUD significa POST/GET na coleção e GET/PUT/DELETE em `/{id}`. Notificações
não tinha CRUD HTTP na main; não foi criado um agora. Os oito CRUDs continuam
com listas e IDs em memória, perdidos ao reiniciar o processo. O serviço de
notificações preserva suas operações sobre objetos sem persistência.

`auth`, `usuaria`, `cyberTalk`, solicitação/análise de educadora, migrações V1–V5
e configuração de datasource permanecem intactos. `talk/ContaApi` antigo e
`schema.sql` como inicializador não foram recuperados. Não há nova migração.

## Limitações anteriores preservadas

- Esses contratos não consultam a sessão atual nem verificam autoria por conta.
  Seus IDs de usuária, educadora e empresa vêm do cliente, sem vínculo validado
  com PostgreSQL. Recuperação de compatibilidade não equivale a autorização nova.
- Cursos e mentorias não consultam aprovação real da educadora. Essa lacuna já
  existia na main; nenhum endpoint antigo de educadora foi reintroduzido para
  alimentar ou contornar o novo fluxo de aprovação.
- Cadastro de empresa em memória recebe `senhaHash` e não cria conta, sessão ou
  papel EMPRESA. O hash continua WRITE_ONLY e não aparece nas respostas JSON.
- Alguns controladores capturam dados inválidos como 400, outros propagam
  IllegalArgumentException; não foi uniformizado o tratamento de erros.
- O validador de certificados confere formato UUID, não existência ou autenticidade
  de um certificado emitido. IDs e relações em memória não têm integridade referencial.
- As telas CyberAcademy/CyberVagas, empresas, cursos, perfil e chat continuam
  indisponíveis. Recuperar APIs não implementa essas telas.

## Educadoras: conflitos e proposta pendente

Nenhum arquivo do CRUD antigo foi recuperado e nenhuma compatibilidade foi
implementada. Comparação do contrato antigo com o fluxo atual:

| Contrato antigo | Conflito | Proposta para revisão, ainda não implementada |
| --- | --- | --- |
| POST `/api/educadoras` | Cadastro paralelo com nome/email/senhaHash, sem conta autenticada | Manter cadastro pessoal e POST `/api/educadora/solicitacao`, que obtém conta da sessão e aceita somente especialidade/instituição |
| GET coleção e GET `/{id}` | Leitura por ID sem autorização; identidade em memória diferente de conta_id | Manter consulta da própria solicitação e listagem/consulta por ADMIN; eventual adaptador deve usar DTO sem credenciais e esclarecer IDs |
| PUT `/{id}` | Altera nome/especialidade/instituição de qualquer registro sem verificar titularidade | Definir edição somente da própria conta, campos permitidos e efeito sobre aprovação antes de criar uma rota |
| DELETE `/{id}` | Exclusão sem titularidade; não define efeito sobre papéis, sessão e histórico | Definir cancelamento/revogação e auditoria; não mapear automaticamente para exclusão de conta |
| POST `/verificar-publicacao` | Apenas recebe status do cliente e retorna uma mensagem; não autoriza publicação real | Se necessário, consultar status salvo e identidade da sessão; nunca aceitar aprovação declarada pelo cliente |
| Métodos de aprovação/rejeição/reanálise do service antigo | Regras de objetos em memória não equivalem às transações e decisões ADMIN atuais | Manter a análise ADMIN atual; discutir reanálise/revogação separadamente, com histórico e transições definidos |

Correção da revisão anterior: o modelo antigo já tinha `@JsonProperty(WRITE_ONLY)`
em `senhaHash`; não há evidência de exposição normal desse campo por serialização
JSON. O problema é receber credencial em cadastro paralelo e não vincular
operações à identidade autenticada. A proposta mantém credenciais fora dos DTOs
profissionais e não aceita status/papéis enviados no cadastro.

O fluxo atual continua sem reenvio, reanálise e revogação. Compatibilidade total
com `/api/educadoras` permanece pendente da definição desses contratos; não deve
ser anunciada como funcionalidade preservada.

## vagasService.js

O arquivo da main contém `...API_URL(token ?{ Authorization})`: expressão ternária
incompleta, tentativa de chamar uma string como função e Authorization sem definição.
Não exporta operações HTTP e não tem importadores ou usos no frontend atual.
Não foi recuperado. A API `/api/vagas` foi recuperada normalmente; quando houver
tela consumidora, usar o cliente compartilhado `services/api.js`, sem restaurar
token legado de localStorage nem inventar contratos frontend sem uso.

## CORS e proxy

Foram conferidos cliente HTTP, chamadas fetch, Vite, compose e documentação.
Os fluxos suportados usam `/api`: proxy Vite em desenvolvimento e encaminhamento
no mesmo domínio na implantação. `API_PROXY` continua selecionando o destino no
servidor Vite; não exige autorização CORS no navegador. Playwright usa esse proxy.

Não foi identificado fluxo suportado dependente de acesso direto de outra origem
à API. Portanto `talk/CorsLocal` não foi recuperado e o proxy não mudou. O cliente
ainda aceita `VITE_API_URL`, mas URL absoluta de outra origem precisaria de política
CORS explícita e validada, incluindo origem/headers/métodos; esse modo não é o
fluxo documentado. Clientes CLI não dependem de CORS. A configuração antiga
autorizava somente `http://localhost:5173`, não qualquer origem de desenvolvimento.

## Validação desta correção

- Maven: 62 testes aprovados, sem falhas, erros ou ignorados: 53 existentes e
  9 novos em `ModulosMainTests`.
- Os novos testes usam MockMvc e controllers/serviços reais sem datasource.
  Cobrem os oito ciclos de criação/listagem/consulta/atualização/exclusão, estado
  em memória, 404, respostas sem senhaHash, validadores auxiliares (200/400) e os
  dois estados de leitura de notificações. Não alegam autorização inexistente.
- Os 53 testes atuais foram executados com cluster PostgreSQL novo e descartável
  em `127.0.0.1:55439`, incluindo bancos separados para adoção/migrações e sessão.
  O banco atual e a demonstração não foram acessados.
- Frontend: 8 testes unitários, lint e build aprovados.
- Conferência por hash Git: os 27 arquivos recuperados são idênticos à main.
  `git diff --check` acusa espaços finais/linhas finais já presentes nesses
  arquivos da main, preservados para manter a recuperação literal. Documentação
  e teste novos não introduzem esses problemas.
- Playwright/Chrome: 7 testes aprovados com API em 18081 e banco novo
  `cyberladies_frontend_e2e` no cluster descartável; cobrem o fluxo atual de
  educadora/ADMIN, sessão, publicação/anexo/interações e navegação mobile.
- Não há validação Firefox/Safari nem ensaio Docker nesta correção.

Para repetir os testes backend, preparar bancos novos conforme `MIGRACOES.md`,
definir DATABASE_URL para `jdbc:postgresql://127.0.0.1:55439/cyberladies_auth_test`,
DATABASE_USER/DATABASE_PASSWORD do cluster descartável e MIGRATION_TESTS=true;
executar `mvn test`. Nunca apontar a suíte ao banco atual. Para Playwright seguir
`docs/VALIDACAO_FRONTEND.md` e usar outro banco novo `cyberladies_frontend_e2e`.
