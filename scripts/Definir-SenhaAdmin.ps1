param([Parameter(Mandatory=$true)][string]$Email)
. (Join-Path $PSScriptRoot 'Apresentacao-Comum.ps1')
$config=Get-AmbienteApresentacao
$null=Get-IdentidadeBancoApresentacao $config
$api=Get-ProcessoPortaApresentacao 18083
$site=Get-ProcessoPortaApresentacao 5178
if(-not $api -or -not $site){throw 'Inicie o ambiente antes de definir a senha e validar o login.'}
Assert-ProcessoApresentacao $api 'api'
Assert-ProcessoApresentacao $site 'frontend'
if([Console]::IsInputRedirected){throw 'Execute diretamente em um terminal interativo, sem pipe ou redirecionamento de entrada.'}
$backend=Join-Path $script:ProjetoApresentacao 'backend'
$repositorioMaven=Join-Path $env:USERPROFILE '.m2/repository'
Push-Location $backend
try{
    & mvn.cmd "-Dmaven.repo.local=$repositorioMaven" -q -DskipTests compile dependency:build-classpath '-Dmdep.outputFile=target/admin-classpath.txt'
    if($LASTEXITCODE -ne 0){throw 'Falha ao compilar o comando operacional; nenhuma senha foi alterada.'}
    $dependencias=(Get-Content -LiteralPath (Join-Path $backend 'target/admin-classpath.txt') -Raw).Trim()
    $classpath=(Join-Path $backend 'target/classes')+[IO.Path]::PathSeparator+$dependencias
    $env:DATABASE_URL='jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao'
    $env:DATABASE_USER=$config.user
    if(-not $env:DATABASE_PASSWORD){$env:DATABASE_PASSWORD=''}
    & java.exe -cp $classpath CyberLadies.auth.AlterarSenhaAdministrador "--email=$Email"
    if($LASTEXITCODE -ne 0){throw 'Comando não concluiu todas as etapas. Confira a mensagem acima antes de repetir.'}
}finally{Pop-Location}
