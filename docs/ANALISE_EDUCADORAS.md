# Análise administrativa de solicitações de educadora

As rotas abaixo exigem sessão válida e papel ADMIN consultado no servidor.
Não existe bypass por campos enviados pelo cliente. O ID usado na URL identifica
a conta solicitante; o administrador responsável vem exclusivamente da sessão.

## Contratos

| Método | Rota | Resposta de sucesso |
| --- | --- | --- |
| GET | `/api/admin/educadoras/solicitacoes` | 200, página de solicitações |
| GET | `/api/admin/educadoras/solicitacoes/{contaId}` | 200, solicitação individual |
| POST | `/api/admin/educadoras/solicitacoes/{contaId}/decisao` | 200, solicitação decidida |

A listagem aceita `status=PENDENTE|APROVADA|REJEITADA` (opcional), `pagina`
(a partir de 0, padrão 0) e `tamanho` (1 a 100, padrão 20). A ordem é pelo ID da
conta crescente. Retorna `itens`, `pagina`, `tamanho` e `total` correspondente ao
filtro, inclusive quando a página está vazia. A leitura de itens e total usa
o mesmo snapshot transacional.

Cada item e resposta individual contém exatamente:

```json
{
  "contaId": 123,
  "nome": "Nome da solicitante",
  "especialidade": "Java",
  "instituicao": "Escola",
  "status": "PENDENTE",
  "motivoRejeicao": null,
  "decididaPor": null,
  "decididaEm": null
}
```

Não são retornados e-mail, hash, token, senha, competências ou participação.

Corpo para aprovar:

```json
{"status":"APROVADA"}
```

Corpo para rejeitar:

```json
{"status":"REJEITADA","motivo":"Explique o motivo da rejeição."}
```

Motivo é obrigatório, não pode ser composto somente de espaços e tem limite
de 1000 caracteres. Espaços nas extremidades são removidos. Em aprovação, não
envie motivo. Campos extras, como responsável, conta ou data, são recusados.
Não há endpoint para reanálise, revogação ou edição da decisão.

## Transação e registro da decisão

O serviço valida ADMIN e grava status, motivo, ID do administrador e data do
PostgreSQL em uma transação. O UPDATE exige `status='PENDENTE'`. Ao aguardar
uma decisão concorrente, PostgreSQL reavalia essa condição: somente uma decisão
é gravada, e a outra recebe conflito. Retentativas não sobrescrevem responsável,
data, motivo ou status. O estado final é lido na mesma transação.

Nenhum papel, sessão ou participação no Talk é alterado. A aprovação é um
requisito a consultar no servidor em uma futura publicação educacional, mas
nenhuma funcionalidade de cursos é implementada nesta etapa.

## Resultado para a própria educadora

`GET /api/educadora/solicitacao` continua usando o ID da sessão. Além dos campos
anteriores (`especialidade`, `instituicao`, `status`), retorna `decididaEm` quando
houver decisão registrada e `motivoRejeicao` quando houver rejeição. Não retorna
o ID do administrador. Os novos campos nulos são omitidos, preservando a
resposta anterior de uma solicitação pendente.

Outra conta não pode consultar esse pedido pela rota própria: qualquer
parâmetro de ID não substitui a identidade autenticada. A consulta administrativa
permanece exclusiva de ADMIN.

## Códigos HTTP

- 200: listagem, consulta ou decisão concluída.
- 400: paginação/filtro inválido, status de decisão inválido, motivo ausente ou
  inválido na rejeição, motivo enviado na aprovação ou campos extras no corpo.
- 401: sessão ausente, inválida ou expirada.
- 403: conta autenticada sem ADMIN nas rotas administrativas.
- 404: solicitação inexistente, após verificar a autorização.
- 409: solicitação já analisada, inclusive disputa entre decisões concorrentes.

Mensagens de domínio: `Acesso restrito a administradores.`, `Solicitação de
educadora não encontrada.`, `Solicitação já analisada.`, `Informe o motivo da
rejeição.`, `Informe APROVADA ou REJEITADA.`. Os erros seguem o ProblemDetail
existente; validações de JSON e parâmetros seguem os erros padrão do Spring.

## Migração e implantação

A V5 acrescenta `decidida_por` (FK para conta), `decidida_em` e
`motivo_rejeicao`, mais índice por status/conta e CHECK de coerência. Nenhuma
migração anterior foi editada. O vínculo impede apagar uma conta responsável
enquanto houver decisões referenciando-a, preservando a identificação histórica.

Se já houver APROVADA/REJEITADA anterior à V5 sem auditoria, ela é preservada
com campos nulos: não é inventado um administrador ou horário nem permitida uma
nova análise desse pedido pela API. Todas as decisões novas feitas pela API
preenchem responsável e data. O CHECK impede auditoria parcial e exige motivo
para uma rejeição com auditoria registrada.

Use backup, ensaio em cópia e atualização coordenada conforme MIGRACOES.md.
Para recuperação, restaure o backup pré-V5 em outro banco e use o artefato
anterior. Não apague histórico nem edite as migrações aplicadas.

## Validação

Os testes PostgreSQL isolados verificam aprovação e rejeição, campos e limites,
responsável/data, sessão e ADMIN em todas as operações, inexistência, conflito
repetido e concorrente, paginação/filtro, consulta própria e preservação de
papéis, sessões e Talk. São executados junto com a suíte anterior; administradores
e solicitantes usados nesses testes são apenas fixtures da instância isolada.
Nenhum banco atual é acessado pelos procedimentos desta entrega.

Resultado da execução desta etapa: **53 testes aprovados, zero falhas, zero
erros e zero testes ignorados**, incluindo sete testes novos de análise
administrativa. A execução usou PostgreSQL 16 isolado e MockMvc para as rotas.
Também passaram os testes anteriores de migrações, administrador, papéis,
cadastro, sessão, solicitação própria e Talk. Docker e restauração operacional
de backup não foram executados; o banco atual não foi acessado.
