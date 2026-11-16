> Histórico da origem. Nesta integração, identidade, migrações e alguns contratos foram reconciliados. Use [INTEGRACAO_APRESENTACAO.md](INTEGRACAO_APRESENTACAO.md) para as regras e comandos atuais; não execute os SQLs/perfis antigos contra o banco da demonstração.

# CyberVagas

## Regras confirmadas

- Consulta pública; candidaturas somente para contas de usuária com sessão válida.
- Reutiliza o Bearer opaco do CyberTalk em sessionStorage. Não é JWT.
- Não exige adesão à comunidade para se candidatar.
- Identidade e status vêm do servidor. Status único nesta etapa: PENDENTE.
- Uma candidatura por usuária/vaga, garantida por UNIQUE no PostgreSQL.
- Consulta de candidaturas restrita à própria conta. Registro de outra conta retorna
  404; alterações retornam 403 após autenticação.
- Vagas encerradas não aparecem e não recebem novas candidaturas.
- Escrita pública de vagas bloqueada. Gestão e autenticação de empresas ficam para
  outra etapa, conforme decisão da usuária.
- Nenhuma empresa/vaga de demonstração é inserida automaticamente.

## API

| Método | Endpoint | Comportamento |
| --- | --- | --- |
| GET | /api/vagas | Consulta pública, resultados e indicadores |
| GET | /api/vagas/{id} | Vaga ativa ou 404 |
| POST | /api/candidaturas | JSON {"vagaId":123}; Bearer obrigatório; retorna 201 |
| GET | /api/candidaturas | Candidaturas da conta autenticada |
| GET | /api/candidaturas/{id} | Somente a própria candidatura |
| POST/PUT/PATCH/DELETE | /api/vagas e /api/vagas/{id} | Bloqueados com 403 |
| PUT/PATCH/DELETE | /api/candidaturas e /api/candidaturas/{id} | Bloqueados após autenticação |

GET /vagas retorna {vagas, totalResultados, vagasDisponiveis, empresas}.
Esse contrato substitui o array do antigo CRUD em memória. Os antigos endpoints
POST /vagas/validar e POST /candidaturas/validar deixam de fornecer validação pública.

Parâmetros:
- busca: até 150 caracteres; título, empresa, descrição, requisitos e tecnologias.
  Ignora maiúsculas e acentos.
- nivel: ESTAGIO, JUNIOR, PLENO, SENIOR. Repetir permite várias opções.
- modalidade: REMOTO, HIBRIDO, PRESENCIAL. Repetir permite várias opções.
- salarioMinimo: 0 a 1000000. Compara o teto da faixa com o piso desejado; quando
  só há piso informado, compara esse piso. Salários ausentes não satisfazem o filtro.
- paraMaes, pcd, semFormacao: booleanos.

Há OR dentro de nível/modalidade e AND entre grupos e inclusões.
Indicadores contam vagas ativas e empresas distintas com vagas ativas antes dos
filtros. Não afirmam parcerias não cadastradas.
Respostas: 400 inválido; 401 sessão ausente/expirada; 403 proibido; 404 ausente;
409 duplicata. Falhas de conexão/servidor aparecem sem fallback para mocks.

## PostgreSQL e carga administrativa

schema.sql cria tabelas de forma aditiva:
- cyber_empresa: identidade e nome público.
- cyber_vaga: filtros, salários, publicação, situação e FK da empresa.
- cyber_candidatura: FKs para talk_usuaria/cyber_vaga e restrição única.

O volume dados mantém os registros entre reinicializações. As listas anteriores
de vagas/candidaturas eram voláteis; não há dados persistentes antigos a migrar.
O CRUD legado /api/empresas permanece fora deste fluxo e ainda usa memória.
A carga de cyber_empresa é a fonte persistente do CyberVagas.
Os modelos record são projeções de dados públicas; candidatura tem DTO de entrada
próprio. Os repositórios usam JdbcClient, seguindo o CyberTalk.

O teste de navegador usa Chrome instalado e Node 22+ sem novas dependências.
Defina CHROME_PATH caso o executável esteja em outro local.
CyberVagasIntegrationTest usa MockMvc e PostgreSQL real. Fixtures são revertidas
ao final de cada teste; não são seed. Sequências podem avançar após rollback.
Execute testes somente em desenvolvimento. A busca atual filtra no servidor após
consultar todas as vagas ativas; paginação/filtragem SQL ficam para maior volume.

Teste manual com dados reais: buscar e combinar filtros; abrir detalhes; tentar
candidatura sem sessão; entrar e confirmar retorno à vaga; enviar, recarregar e
verificar duplicata. Confira teclado, leitor de tela, dispositivos físicos, cadastro
de conta nova e anexos do CyberTalk.