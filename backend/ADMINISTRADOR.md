# Criação controlada de administrador

O comando cria uma conta dedicada com somente ADMIN. Não cria registros em
`usuaria`, `educadora` ou `talk_participacao`, não transforma uma conta existente
e não abre endpoint HTTP. Não há senha padrão ou criação na inicialização.
Não é necessária uma nova migração: V1–V4 permanecem intactas.

## Preparar e identificar o destino

Use Java 21 e um terminal interativo real. O banco deve estar previamente
migrado pela aplicação atual. O comando não inicia Spring, servidor ou Flyway.

Antes de operar, confira host, porta e nome do banco, use as credenciais
operacionais autorizadas e siga a política de backup do ambiente. Nesta entrega
nenhum administrador foi criado no banco de desenvolvimento.

Defina explicitamente `DATABASE_URL` (JDBC PostgreSQL), `DATABASE_USER` e
`DATABASE_PASSWORD`. Estas são as credenciais do **banco**, não a senha da conta
administrativa. Não versione valores, não coloque senhas em argumentos e não
habilite transcrição/gravação de terminal ou logs de diagnóstico nesta operação.

## Executar (PowerShell, dentro de backend)

```powershell
.\mvnw.cmd compile dependency:build-classpath '-Dmdep.outputFile=target/admin-classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'Compilação falhou.' }
$adminClasspath = 'target/classes;' + (Get-Content target/admin-classpath.txt -Raw).Trim()

# Único argumento permitido: o nome EXATO do banco previamente conferido.
java -cp $adminClasspath CyberLadies.auth.CriarAdministrador --banco=NOME_EXATO_DO_BANCO
if ($LASTEXITCODE -ne 0) { throw 'Criação não concluída. Confira antes de repetir.' }
```

O comando compara o nome informado com `current_database()` e exibe banco,
servidor e porta conectados. Depois pede nome, e-mail, senha e confirmação. A
senha é lida com `Console.readPassword`, sem eco; não é aceita por argumentos,
variável de ambiente, arquivo ou pipe. Sem console protegido (por exemplo, em
alguns consoles de IDE ou com entrada redirecionada), a execução é recusada.
Use outro terminal, sem substituir a leitura protegida por entrada comum.

O nome deve ter até 120 caracteres e o e-mail até 254. O e-mail é normalizado
com remoção de espaços nas extremidades e conversão para minúsculas. A senha
deve ter entre 8 e 128 caracteres, não pode ser composta somente de espaços e
seus espaços significativos são preservados. O comando usa a mesma fábrica de
PasswordEncoder de cadastro e login (`SenhasConfig`); não contém outra
configuração de hash.

Conta e papel são inseridos em uma transação usando conexão dedicada e
JdbcClient. E-mail existente é recusado, incluindo diferenças apenas de caixa
ou espaços na entrada, sem promover ou alterar a conta. Falha na atribuição do
papel reverte também a conta. Uma tentativa falha pode consumir um número da
sequência, comportamento normal do PostgreSQL, sem deixar uma conta parcial.

Só há mensagem de sucesso após o commit. Erros não imprimem stacktrace,
parâmetros JDBC, senha ou hash. Os arrays de senha e confirmação são apagados
ao sair do comando; isso não é garantia de eliminação de todas as cópias internas
que bibliotecas/JVM possam produzir. Em erro de conexão durante o commit, confira
se a conta foi confirmada antes de repetir a operação. Não existe promoção
silenciosa, senha fixa ou recuperação automática de senha nesta etapa.

No Compose atual o banco não publica porta para o host. Caso futuramente use
esse ambiente, compile o classpath dentro do container Linux e execute Java com
`docker compose run --rm --no-deps api ...`, mantendo o TTY interativo. O classpath
Linux usa `:` em vez de `;` e caminhos internos do container. Não reutilize um
classpath Windows dentro do container. Essa execução Docker não foi validada
nesta entrega; não forneça a senha por pipe para contornar falta de console.

## Autenticar e conferir

Use o login existente: `POST /api/auth/login`, JSON com `email` e `senha`. Digite
a senha somente em um cliente de API com campo protegido e logging de corpo
desabilitado; não a coloque em comandos de shell, arquivos de exemplo ou logs.
O resultado permanece `{"token":"..."}`, com expiração de oito horas.

Envie esse token em `Authorization: Bearer <token>` para `GET /api/auth/me`.
A resposta deve conter somente ID e `papeis: ["ADMIN"]`. Trate o token como
segredo também. O logout existente, `DELETE /api/auth/sessao`, encerra a sessão.

ADMIN não herda USUARIA, EDUCADORA ou EMPRESA e não recebe aprovação,
participação no Talk ou bypass de autoria. Pode usar somente as operações
administrativas implementadas e autorizadas explicitamente, atualmente a
[análise de solicitações de educadora](ANALISE_EDUCADORAS.md). Não há moderação.
A solicitação de educadora é recusada
com 403 por exigir USUARIA. As rotas atuais de perfil/Talk retornam 401 para
essa conta, pelo contrato existente que exige dados de usuária; isso não invalida
o login nem altera o resultado de `/auth/me`. O frontend atual não possui área
administrativa e não foi alterado.

## Validação e limites

Os testes PostgreSQL isolados exercitam a operação transacional com senhas
aleatórias exclusivas das fixtures, hash compatível, login, `/auth/me`, ausência
de outros perfis/participação, duplicidade, rollback provocado na atribuição e
destino incorreto. Conferem que o cadastro público não concede ADMIN e que as
rotas dos demais perfis continuam restritas. Um subprocesso Java verifica a
recusa quando não há console interativo.

O fluxo completo de digitação humana com eco oculto depende de um terminal
compatível e não é automatizado pela suíte. O comando usa diretamente a API
protegida do Java, sem fallback para entrada com eco. A criação real no banco
atual e a execução Docker não foram realizadas.
