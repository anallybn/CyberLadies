# Ambiente local persistente

O ambiente local usa sempre o banco **existente** `cyberladies_apresentacao`,
na mesma instância PostgreSQL em **127.0.0.1:55439**. Frontend em
**http://127.0.0.1:5178** e API em **127.0.0.1:18083**, pelo proxy `/api`.

Na raiz do projeto, em PowerShell, use somente estes comandos de operação:

| Ação | Comando |
| --- | --- |
| Iniciar, inclusive depois de reiniciar o Windows | `.\scripts\Iniciar-Apresentacao.ps1` |
| Parar frontend, API e a mesma instância PostgreSQL | `.\scripts\Parar-Apresentacao.ps1` |

Iniciar não cria banco, cluster, conta, papel ou senha. Se o PostgreSQL estiver
parado, inicia somente seu diretório persistente cadastrado. Se o diretório,
configuração, banco ou acesso estiver indisponível, informa erro e não cria nem
restaura uma alternativa. Repita o comando após corrigir o problema. Não executar
scripts antigos de preparação de demonstração ou recriação de fixtures.

## Armazenamento permanente neste computador

Tudo que precisa sobreviver à limpeza do projeto fica em
`%LOCALAPPDATA%\CyberLadies\apresentacao`, fora do repositório, de TEMP, target e dist:

| Caminho nessa pasta | Conteúdo |
| --- | --- |
| `postgresql\data` | Diretório original do cluster, movido após parada limpa; todos os seus bancos preservados |
| `ambiente.json` | Localização fixa e identidade do cluster/banco: portas, system identifier e OID; sem senha |
| `backups\<data-hora>` | Backup lógico integral, backup físico e evidências da recuperação/comparação |
| `logs` | Logs de PostgreSQL, API e frontend |
| `ACESSOS-LOCAIS.md` | Anotações de acesso já existentes, movidas de target sem alteração de credenciais; não versionar |

Os scripts não dependem mais de um apontador em TEMP ou de PIDs de uma sessão
anterior. Conferem `PG_VERSION`, diretório, identificador do cluster e OID do
banco, além dos processos que ocupam as portas. Não encerram processos alheios.
O armazenamento continua no disco após parar ou reiniciar o Windows; execute o
mesmo comando de iniciar após entrar novamente. Não foi configurado início
automático com o Windows nem realizado reboot do computador durante a validação.

O JAR continua sendo um artefato descartável em `backend/target`; dependências
do frontend continuam em `frontend/node_modules`. Se comandos de limpeza os
removerem, recompilar/reinstalar essas dependências antes de iniciar. A ausência
desses artefatos gera erro claro e não provoca criação ou substituição do banco.
Não use o banco de apresentação para executar os testes ao recompilar.

## Preservação e recuperação verificada

Antes da mudança de armazenamento, os serviços da aplicação foram parados para
impedir escritas. Foram realizados:

1. Backup lógico integral de **todos os bancos e papéis** do cluster existente.
2. Backup físico com WAL e verificação pelo `pg_verifybackup`.
3. Recuperação real de uma cópia do backup em instância temporária de teste na
   porta 55441, sem substituir o banco principal.
4. Comparação privada do conteúdo lógico integral recuperado com o original,
   incluindo contas, hashes de senha, papéis, relações, sequências e demais dados.
   Nenhum desses valores sensíveis foi exibido ou colocado no Git.
5. Encerramento da instância de recuperação. Só depois da confirmação, parada
   limpa da instância original e movimentação de seu próprio diretório para a
   pasta permanente, sem usar o backup como substituto.
6. Verificação da mesma identidade do cluster e OID de `cyberladies_apresentacao`,
   e comparação integral após iniciar no caminho novo.

Todos os bancos que já estavam nesse cluster foram mantidos. A instância
anterior na porta 55440 não foi parada, apagada ou alterada. Nenhuma conta foi
recriada e nenhuma senha, papel ou dado foi redefinido.

O backup contém dados sensíveis e fica somente na pasta local permanente. Não
incluir essa pasta no Git. Para qualquer futura movimentação de armazenamento,
repetir backup e ensaio de recuperação antes; os scripts de iniciar/parar nunca
executam restauração ou migração de armazenamento automaticamente.

## Verificação do ciclo de operação

Foi executado o comando de parar, confirmando ausência dos serviços nas portas
5178, 18083 e 55439, seguido pelo comando de iniciar. Os dumps lógicos completos
antes/depois foram equivalentes, preservando todos os bancos, papéis e conteúdo,
sem revelar senhas ou hashes. Também foram verificadas chamadas repetidas dos
dois comandos e a recusa de configuração ausente, sem criação automática.
O ambiente foi deixado **iniciado** ao terminar.

## Bancos de testes

`cyberladies_auth_test`, `cyberladies_frontend_e2e` e os bancos
`cyberladies_migracao_*` continuam separados para testes automatizados. As
fixtures e rotinas destrutivas de teste não devem apontar para
`cyberladies_apresentacao`. Não há criação de outro banco de demonstração durante
a inicialização. O ensaio de recuperação usa uma instância temporária somente
para validação automatizada do backup, encerrada após a conferência.
