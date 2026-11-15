# Migrações do PostgreSQL

O projeto usa Spring Boot 4.1.1 e Flyway 12.4.0, na versão gerenciada pelo Boot.
`db/migration/V1__schema_inicial.sql` representa o schema anterior sem alterar o
modelo. V1 não contém `IF NOT EXISTS`: inconsistências precisam interromper a
operação. Depois de aplicada, V1 é imutável; mudanças futuras recebem V2, V3 etc.

Flyway é o único inicializador: `spring.sql.init.mode=never`, Hibernate com
`ddl-auto=none`, baseline automático desligado, validação ligada e limpeza
desabilitada. O antigo `schema.sql` só existe como fixture de teste em
`src/test/resources/db/schema-anterior.sql`, fora da inicialização da aplicação.

## Banco vazio

Configure `DATABASE_URL`, `DATABASE_USER` e `DATABASE_PASSWORD` e inicie a
aplicação normalmente. Flyway aplica V1 a V5, nessa ordem, e registra suas
execuções em `public.flyway_schema_history`. Reinícios validam o histórico e
não reaplicam as migrações.

Um banco preenchido e sem histórico é recusado. Não contorne essa proteção com
`baseline-on-migrate=true`, `repair` ou execução manual de V1 sobre as tabelas.

## Adoção futura de um banco de desenvolvimento existente

Este procedimento NÃO foi aplicado ao banco de desenvolvimento nesta etapa.

1. Reserve uma janela sem uso, pare todas as instâncias da API e qualquer outro
   processo que escreva dados ou altere estrutura. Identifique host, porta,
   banco e usuário. Não execute a adoção contra uma URL implícita ou presumida.
2. Faça backup completo e ensaie sua restauração em outro banco. Confira dados,
   anexos e vínculos na cópia restaurada; execute primeiro nela a conferência e
   a adoção descritas abaixo. Uma listagem do arquivo não substitui esse ensaio.
3. Compile a ferramenta, confira o banco de destino e execute `--verificar`.
   Se houver divergência, pare e investigue; não altere a assinatura esperada
   apenas para permitir o baseline.
4. Somente com a conferência aprovada e a aplicação ainda parada, execute
   `--adotar=NOME_EXATO_DO_BANCO`. O comando repete a conferência antes de
   registrar o baseline V1. Não aplica V1 nem altera tabelas de negócio.
5. Inicie a API, confira cadastro, login, sessão existente, publicações,
   comentários, curtidas e download dos anexos. Confira o histórico e reinicie
   novamente. Para um banco adotado haverá uma linha V1 do tipo `BASELINE`;
   para um banco novo, uma linha V1 do tipo `SQL`, com checksum. Ao iniciar a
   aplicação atual, também haverá linhas V2 a V5 do tipo `SQL` nos dois casos.

O verificador e o comando de adoção continuam específicos do schema anterior
à separação de contas. Não devem ser usados em um banco já na V1 com histórico
Flyway ou na V2. Um banco já versionado na V1 segue diretamente o procedimento
de atualização coordenada abaixo, sem novo baseline.

### Backup e conferência do destino (PowerShell)

Use os valores reais do ambiente, definidos explicitamente. `PGHOST`, `PGPORT`
e `PGDATABASE` devem corresponder a `DATABASE_URL`. Use o mesmo major do
PostgreSQL ou ferramentas de backup compatíveis. Não versione backups ou senhas.

```powershell
# Já definidos pelo operador: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD,
# PGHOST, PGPORT e PGDATABASE. DATABASE_URL tem o prefixo jdbc:postgresql://.
foreach ($nome in @('DATABASE_URL','DATABASE_USER','DATABASE_PASSWORD','PGHOST','PGPORT','PGDATABASE')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($nome))) {
        throw "Defina explicitamente $nome antes de continuar."
    }
}
$env:PGUSER = $env:DATABASE_USER
$env:PGPASSWORD = $env:DATABASE_PASSWORD
psql -X -v ON_ERROR_STOP=1 -c 'SELECT current_database(), current_user, inet_server_addr(), inet_server_port();'
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível conferir o destino.' }

$backupDir = Join-Path $env:USERPROFILE 'BackupsCyberLadies'
New-Item -ItemType Directory -Force $backupDir | Out-Null
$backupFile = Join-Path $backupDir ('cyberladies-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')
pg_dump --format=custom --file=$backupFile
if ($LASTEXITCODE -ne 0) { throw 'Backup falhou. Não prossiga.' }
pg_restore --list $backupFile
if ($LASTEXITCODE -ne 0) { throw 'Backup não pôde ser lido. Não prossiga.' }
```

Restaure esse arquivo com `pg_restore --exit-on-error --dbname=NOME_DA_COPIA`
em um banco novo, criado separadamente com `createdb`. Confira também os
proprietários e permissões necessários ao ambiente. Não restaure sobre o banco
original. Remova `PGPASSWORD` do ambiente ao concluir os comandos operacionais.

### Ferramenta explícita de adoção (PowerShell, dentro de backend)

A ferramenta exige as três variáveis `DATABASE_*`, sem valores padrão. Ela não
inicia o Spring nem faz parte da inicialização automática da aplicação.

```powershell
.\mvnw.cmd compile dependency:build-classpath '-Dmdep.outputFile=target/migration-classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'Compilação falhou.' }
$migrationClasspath = 'target/classes;' + (Get-Content target/migration-classpath.txt -Raw).Trim()

java -cp $migrationClasspath CyberLadies.database.AdotarBanco --verificar
if ($LASTEXITCODE -ne 0) { throw 'Schema incompatível. Não prossiga.' }

# Substitua pelo nome EXATO previamente conferido, sem depender de um valor padrão.
java -cp $migrationClasspath CyberLadies.database.AdotarBanco --adotar=NOME_EXATO_DO_BANCO
if ($LASTEXITCODE -ne 0) { throw 'Adoção não concluída. Investigue antes de iniciar.' }

.\mvnw.cmd spring-boot:run
```

O nome passado a `--adotar` é comparado com `current_database()`. A inspeção usa
uma transação somente de leitura e locks de leitura nas tabelas existentes;
somente o Flyway escreve sua tabela de histórico durante a adoção. Esses locks
não substituem a janela sem uso nem impedem todas as alterações administrativas.
O usuário precisa ler os catálogos/tabelas e criar o histórico no schema public.

### Ambiente Docker Compose

No Compose atual, o serviço `banco` não publica a porta PostgreSQL no host.
Não use os comandos locais com `localhost:5432` presumindo que apontam para esse
volume. Faça backup e restauração com as ferramentas do container PostgreSQL e
execute a ferramenta Java em um container na mesma rede. Não é necessário
alterar `compose.yaml` para publicar a porta.

Exemplos para PowerShell na raiz do projeto, somente quando for realizar a
adoção futura, após identificar o volume correto:

```powershell
docker compose stop api site
docker compose up -d banco
docker compose exec -T banco psql -X -U cyberladies -d cyberladies -v ON_ERROR_STOP=1 -c 'SELECT current_database(), current_user, inet_server_addr(), inet_server_port();'
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível conferir o destino Docker.' }

# Arquivo intermediário dentro do container; copie o backup para fora dele.
docker compose exec -T banco pg_dump -U cyberladies -d cyberladies -Fc -f /tmp/cyberladies-antes-flyway.dump
if ($LASTEXITCODE -ne 0) { throw 'Backup falhou.' }
$dockerBackup = 'cyberladies-antes-flyway-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump'
docker compose cp banco:/tmp/cyberladies-antes-flyway.dump $dockerBackup
if ($LASTEXITCODE -ne 0) { throw 'Cópia do backup falhou.' }

# Antes de prosseguir: restaure e ensaie a adoção em OUTRA instância isolada.
# O container avulso herda DATABASE_* de api; não inicia a API nem suas dependências.
docker compose run --rm --no-deps api mvn compile dependency:build-classpath '-Dmdep.outputFile=target/migration-classpath-linux.txt'
if ($LASTEXITCODE -ne 0) { throw 'Compilação falhou.' }
docker compose run --rm --no-deps api bash -c 'java -cp "target/classes:$(cat target/migration-classpath-linux.txt)" CyberLadies.database.AdotarBanco --verificar'
if ($LASTEXITCODE -ne 0) { throw 'Schema incompatível. Não prossiga.' }
docker compose run --rm --no-deps api bash -c 'java -cp "target/classes:$(cat target/migration-classpath-linux.txt)" CyberLadies.database.AdotarBanco --adotar=cyberladies'
if ($LASTEXITCODE -ne 0) { throw 'Adoção não concluída.' }
docker compose up -d api site
```

Os nomes e usuário acima correspondem ao `compose.yaml` atual; confira-os antes
do uso. O backup completo e o ensaio de restauração continuam obrigatórios.
Esses comandos Docker foram revisados, mas não executados nesta validação.

O verificador compara a estrutura com `db/schema_v1.assinatura`, obtida da V1
em PostgreSQL 16. Confere tabelas, colunas, tipos, tamanhos, nulabilidade,
defaults, identidades, chaves, ações de exclusão, índices e configuração/vínculo
das sequências. Relações e colunas adicionais, triggers, rotinas, outros schemas
de usuário e um histórico Flyway já existente causam recusa. Não lê senhas ou dados de
negócio para essa comparação. Proprietários, grants e configurações do servidor
precisam de conferência operacional separada, assim como extensões, policies e
rules personalizadas, que não são integralmente cobertas por essa assinatura.

A comparação é conservadora: nomes de constraints diferentes, objetos extras
ou diferenças de representação de catálogos entre versões do PostgreSQL podem
causar recusa mesmo se houver equivalência funcional. A ferramenta foi validada
com PostgreSQL 16; outros majors precisam de ensaio antes da adoção. Não é um
verificador de integridade física nem substitui backup e restauração.

Após o baseline, Flyway valida o histórico/checksums das migrações, não toda
alteração manual possível no schema. Não edite V1 nem sua assinatura para
introduzir mudanças. Um banco já adotado não deve receber baseline novamente.

## Testes reproduzíveis em ambiente isolado

Use uma instância PostgreSQL descartável em `127.0.0.1:55439`, nunca a instância
de desenvolvimento. Crie bancos novos: `cyberladies_auth_test`,
`cyberladies_migracao_vazia`, `cyberladies_migracao_legado` e
`cyberladies_migracao_v1` e `cyberladies_migracao_v2`.

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55439/cyberladies_auth_test'
$env:DATABASE_USER = 'USUARIO_DA_INSTANCIA_ISOLADA'
$env:DATABASE_PASSWORD = 'SENHA_DA_INSTANCIA_ISOLADA'
$env:MIGRATION_TESTS = 'true'
.\mvnw.cmd clean test
```

Os testes de migração exigem bancos novos e vazios a cada execução. Eles não
apagam bancos nem usam Flyway clean. Para repetir, use novos bancos com esses
nomes em outra instância descartável. A suíte comum pode ser repetida no banco
`cyberladies_auth_test`, com `MIGRATION_TESTS` ausente. Os testes de migração
ficam ignorados sem a opção explícita; os testes existentes de sessão/Talk também
exigem a URL isolada acima.

Os quatro cenários de migração iniciam a aplicação e exercitam as rotas com
MockMvc: banco vazio, banco legado adotado e bancos versionados na V1 e V2. Comparam
os dados anteriores com a projeção equivalente das tabelas separadas, conteúdo
binário dos anexos, valores das sequências e histórico antes/depois dos
reinícios. Há contas participantes e não participantes, competências vazias e
com espaços, sessões válidas e expirada e uma lacuna na sequência de contas.
O teste confere ausência das tabelas/colunas antigas, vínculos para conta,
cadastro sem reiniciar a sequência, adesão idempotente e recusa de alteração de
publicação alheia. O cenário legado
usa uma cópia independente do schema anterior e testa recusas por índice
ausente, nulabilidade alterada e chave estrangeira ausente. Login e cadastro
continuam sendo testados, inclusive hashes e tokens anteriores.

## V2: conta, usuária e participação separadas

`V2__separar_conta_usuaria_e_participacao.sql` executa, em uma transação:

1. Renomeia `talk_usuaria` para `conta` e `talk_sessao` para `auth_sessao`.
2. Cria `usuaria(conta_id,competencias)` e copia uma linha para cada conta.
3. Cria `talk_participacao(conta_id)` e copia somente as contas com `membro=true`.
4. Remove `competencias` e `membro` de `conta`, depois de transferir os dados.

As referências existentes passam a apontar para `conta` pela própria renomeação.
IDs, senhas, hashes de token, expirações e dados de publicações não são
reescritos. O nome da sequência `talk_usuaria_id_seq` e nomes antigos de
constraints são mantidos deliberadamente, assim como `usuaria_id` nas sessões
e curtidas, para limitar a alteração. A sequência permanece vinculada à
identidade de `conta.id`, sem reinício ou ajuste para `max(id)`.

Todas as contas atuais continuam tendo os dados de usuária. A chave
`usuaria.conta_id` identifica esses dados; não existe coluna de perfil nem uma
restrição de exclusividade entre perfis. Participação é representada pela
existência da linha, e não por um booleano na identidade autenticada.

O cadastro usa uma única instrução SQL com CTE para inserir conta e competências:
se a segunda inserção falhar, a primeira não permanece gravada. As rotas e
respostas continuam iguais às anteriores.

### Atualização coordenada da aplicação e do banco

Este procedimento está preparado para uso futuro; o banco atual não foi
acessado nesta etapa. Aplicação anterior e aplicação nova não devem operar
simultaneamente sobre o mesmo banco: a anterior usa as tabelas antigas, e a
nova exige V2. Não faça uma atualização gradual com instâncias misturadas.

1. Identifique explicitamente o destino. Pare todas as instâncias da API e
   quaisquer escritores/alterações administrativas. Pause o site para evitar
   requisições durante a manutenção.
2. Guarde o artefato executável, configuração e fontes exatos da versão
   anterior. Como há alterações locais ainda sem commit, não dependa de um
   checkout de branch para recuperar o programa anterior.
3. Faça backup completo conforme o procedimento acima, incluindo o histórico
   Flyway, e teste a restauração em outro banco. Nunca inicie a aplicação nova
   antes do backup. Ela executa a V2 automaticamente na inicialização.
4. Confira o histórico: banco na V1 deve ter V1 bem-sucedida, sem migração
   falha. Se ainda não houver histórico, faça primeiro a adoção explícita do
   schema anterior. Não execute o verificador legado em um banco já versionado.
5. Ensaie a atualização com o artefato novo na cópia restaurada. Compare IDs,
   contagens, competências, participação, sessões e dados do Talk. Depois,
   usando o destino real explicitamente conferido, inicie apenas a aplicação
   nova. Flyway valida V1 e executa V2 em uma transação PostgreSQL.
6. Confira o histórico e as funcionalidades, reinicie para confirmar que nada
   é reaplicado e só então libere o site e o tráfego.

Consultas de conferência, com `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e
credenciais explicitamente definidos, antes/depois conforme indicado:

```sql
-- Antes e depois:
SELECT installed_rank,version,type,checksum,success
FROM public.flyway_schema_history ORDER BY installed_rank;

-- Antes, na V1: registre também as contagens das demais tabelas do Talk.
SELECT count(*) AS contas, count(*) FILTER (WHERE membro) AS participantes
FROM public.talk_usuaria;
SELECT sequencename,last_value FROM pg_sequences WHERE schemaname='public';

-- Depois, na V2: compare contas=usuarias e participantes com o valor anterior.
SELECT (SELECT count(*) FROM public.conta) AS contas,
       (SELECT count(*) FROM public.usuaria) AS usuarias,
       (SELECT count(*) FROM public.talk_participacao) AS participantes;
SELECT sequencename,last_value FROM pg_sequences WHERE schemaname='public';
```

Não publique hashes ou dumps de dados nos logs da aplicação. As contagens são
uma conferência inicial; o ensaio deve verificar também os valores e vínculos.

### Recuperação da versão anterior

Se V2 falhar, o PostgreSQL deve reverter a transação da migração. Mantenha o
ambiente parado, investigue o erro e confira o histórico; não use `repair`,
baseline ou edições de V1/V2 como tentativa automática de correção.

Se V2 já foi concluída e for necessário retornar, não basta voltar o binário e
não há uma migração inversa automática. Pare a aplicação nova e restaure o
backup anterior em um banco **novo**, mantendo o banco atualizado preservado
para investigação. Exemplo PowerShell, com ferramentas PostgreSQL no PATH:

```powershell
# PGHOST/PGPORT/PGUSER/PGPASSWORD explícitos; $backupFile é o backup pré-V2.
# Escolha um nome de recuperação que ainda não exista.
$recoveryDb = 'cyberladies_recuperacao_v1'
createdb --template=template0 --owner=$env:PGUSER $recoveryDb
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o banco novo de recuperação.' }
pg_restore --exit-on-error --single-transaction --dbname=$recoveryDb $backupFile
if ($LASTEXITCODE -ne 0) { throw 'Restauração falhou. Não libere a aplicação.' }
psql -X --dbname=$recoveryDb -v ON_ERROR_STOP=1 -c 'SELECT version,type,success FROM public.flyway_schema_history ORDER BY installed_rank;'
```

O backup de um banco V1 deve restaurar V1 e suas tabelas antigas, sem V2. Se o
backup for de um banco sem histórico, preserve esse estado e use o artefato
compatível com ele. Aponte `DATABASE_URL` da **aplicação anterior** para o banco
restaurado, confira login, sessão e Talk e só então reabra o tráfego. Não inicie
o artefato novo nesse banco durante a recuperação, pois ele aplicaria V2 outra
vez. Mudanças feitas depois do backup não aparecem na restauração; por isso a
janela sem escritas deve começar antes do backup e durar até a liberação.

Os testes automatizados validam migração e reinícios, mas não executam
`pg_dump`/`pg_restore` nem o procedimento Docker. O ensaio operacional de backup
e recuperação no ambiente de destino continua obrigatório.

## V3: papéis da conta

`V3__papeis_da_conta.sql` adiciona `conta_papel(conta_id,papel)`, com chave
composta e chave estrangeira para conta. São aceitos apenas `USUARIA`,
`EDUCADORA`, `EMPRESA` e `ADMIN`. A migração copia todas as contas existentes
com **somente USUARIA**. Não atribui nenhum dos outros papéis.

Uma conta pode ter USUARIA e EDUCADORA simultaneamente. EMPRESA não pode
coexistir com nenhum desses papéis pessoais, nos dois sentidos da atribuição.
CHECK e chave primária validam os valores e impedem repetição. Um trigger
valida INSERT e UPDATE, inclusive múltiplas linhas, para impedir a combinação
incompatível também em gravações diretas no banco. DELETE pode retirar papéis;
não há concessão implícita de outro papel.

Para evitar atribuições concorrentes incompatíveis, o trigger atualiza
`conta.nome` para o próprio valor, sem mudar o dado. Isso serializa as gravações
da mesma conta e cria uma versão MVCC para que REPEATABLE READ não permita
write skew. Uma transação concorrente é recusada por combinação inválida ou
falha de serialização; no futuro, um fluxo autorizado deve tratar essa falha
sem presumir que a atribuição foi concluída. Os testes usam duas conexões e
confirmam o bloqueio e a recusa em READ COMMITTED e REPEATABLE READ.

O cadastro público fixa USUARIA no SQL, na mesma instrução atômica da criação
da conta e dos dados de usuária. Campos extras enviados pelo cliente não
definem papéis, aprovação ou adesão. A V3 não introduziu endpoint genérico de
atribuição, solicitação de educadora, cadastro de empresa nem criação de ADMIN.
As atribuições adicionais feitas nos testes são fixtures SQL em banco isolado.

`GET /api/auth/me` exige sessão válida e devolve somente:

```json
{"id":123,"papeis":["USUARIA","EDUCADORA"]}
```

`auth` consulta os papéis no banco a cada identificação autenticada; eles não
vêm do token, do payload do cliente nem de um cache de login. A identidade
imutável disponibiliza `possui(Papel)` para futuras verificações no servidor.
O login continua devolvendo apenas `{"token":"..."}`, sem mudança de formato
ou expiração. Sessões anteriores continuam válidas. A nova rota retorna 401
para sessão ausente, inválida ou expirada e não retorna credenciais, competências,
aprovação nem participação no Talk.

Não foi criada uma permissão de publicação de vagas/cursos apenas pela presença
de um papel. Aprovação de parceiros e participação no Talk continuam separadas.
Empresas também poderão oferecer e gerenciar cursos, mas a regra de aprovação
para esses cursos permanece pendente. ADMIN não ganha nesta etapa bypass de
participação/autoria ou qualquer nova operação administrativa. A criação de uma
conta dedicada ADMIN agora é feita pelo [comando operacional](ADMINISTRADOR.md),
nunca pelo cadastro público.

A atualização V2→V3 exige a mesma janela sem escritas, backup e ensaio em cópia
descritos acima. Para voltar à aplicação V2, restaure o backup pré-V3 em um
banco novo e use o artefato anterior; não edite V1/V2 nem apague o histórico.
Os testes conferem atribuição somente de USUARIA na migração, acumulação de
papéis pessoais, rejeição de combinações em INSERT/UPDATE/lote e concorrência,
tentativas de elevação pelo cadastro, consulta de papéis com sessão antiga e
recusa de `/auth/me` sem sessão válida. Nenhum procedimento de atribuição real
de EDUCADORA, EMPRESA ou ADMIN foi executado no banco de desenvolvimento.

## V4: solicitação de atuação como educadora

`V4__solicitacao_educadora.sql` cria `educadora`, ligada à conta por sua chave
primária `conta_id`. Não cria solicitações ou atribui papéis automaticamente às
contas existentes. Não altera V1, V2 ou V3.

Novas rotas, ambas restritas à própria conta autenticada:

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | `/api/educadora/solicitacao` | 201 com dados e status inicial PENDENTE |
| GET | `/api/educadora/solicitacao` | 200 com a solicitação atual; 404 se não existir |

Entrada aceita somente `especialidade` (obrigatória, até 120 caracteres) e
`instituicao` (obrigatória, até 200 caracteres). Textos em branco são recusados;
espaços nas extremidades são removidos após validação. Campos extras, incluindo
`conta_id`, `contaId`, `status`, `papeis`, nome, e-mail ou senha, são recusados
com 400. Não existe uma rota com ID de outra conta.

```json
{"especialidade":"Java","instituicao":"Escola"}
```

Resposta de criação e consulta:

```json
{"especialidade":"Java","instituicao":"Escola","status":"PENDENTE"}
```

O serviço obtém o ID exclusivamente da sessão, bloqueia essa conta para
serializar solicitações e reconsulta seus papéis após o bloqueio. A conta deve
ter USUARIA e não ter EMPRESA. Perfil profissional e papel EDUCADORA são
gravados na mesma transação. Nome, e-mail, senha, sessões, papel USUARIA e
participação no Talk são preservados. Um papel EDUCADORA já presente sem perfil
não é duplicado; sua presença sozinha não constitui aprovação.

Uma solicitação já existente retorna 409, sem substituir os dados ou reiniciar
seu status, inclusive APROVADA ou REJEITADA. Esses valores estão previstos no
CHECK para preservar a separação entre papel e aprovação. A V4 não introduziu
operação para aprovar, rejeitar ou reanalisar; a análise inicial foi acrescentada
na V5, sem reanálise. Solicitações
concorrentes da mesma conta resultam em uma criação e um conflito.

Sessão ausente/inválida/expirada retorna 401. Conta sem USUARIA ou com EMPRESA
retorna 403. Consulta sem solicitação retorna 404. A participação no Talk não
é exigida para solicitar; tampouco é concedida pela solicitação. Nenhuma
permissão de publicar cursos é concedida pela atribuição de EDUCADORA.

O módulo `educadora` contém controller, DTOs, service transacional e repository,
todos internos ao pacote. Consome somente a identidade pública de `auth`.

Os testes em PostgreSQL isolado exercitam os contratos, dados inválidos,
campos proibidos, sessão, empresa, duplicidade nos três estados, concorrência,
isolamento entre contas e preservação de login/Talk. Um trigger exclusivo da
fixture provoca falha ao inserir o papel, após inserir o perfil: o teste
confirma que a transação não deixa nem o perfil nem o novo papel gravados.
O trigger de falha é removido ao terminar o teste e não integra as migrações.

Para implantar V4, use a janela coordenada e o backup descritos acima. O retorno
à aplicação V3 exige restaurar o backup pré-V4 em outro banco e executar o
artefato anterior; não altere migrações antigas nem apague o histórico Flyway.
Nenhum banco de desenvolvimento foi acessado para esta implementação.

## V5: análise administrativa de educadoras

A V5 registra administrador, data e motivo da rejeição sem alterar V1–V4 ou
conceder papéis/permissões adicionais. Consulte os [contratos, erros, regras de
concorrência e tratamento de decisões legadas](ANALISE_EDUCADORAS.md).
O procedimento de atualização coordenada e recuperação por backup continua
obrigatório. Nenhuma aprovação foi executada no banco atual.

## Referências

- [Inicialização de banco no Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Baseline explícito do Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline)
- [Riscos do baseline automático](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting)
