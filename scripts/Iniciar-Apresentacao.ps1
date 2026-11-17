. (Join-Path $PSScriptRoot 'Apresentacao-Comum.ps1')
$config = Get-AmbienteApresentacao
$jar = Join-Path $script:ProjetoApresentacao 'backend/target/cyberladies-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Artefato da API ausente: compile o backend. Os dados persistentes não foram alterados.' }
if (-not (Test-Path -LiteralPath (Join-Path $script:ProjetoApresentacao 'frontend/node_modules/vite/bin/vite.js'))) {
    throw 'Dependências do frontend ausentes: execute npm ci em frontend. O banco existente não foi alterado.'
}
$api = Get-ProcessoPortaApresentacao 18083
$frontend = Get-ProcessoPortaApresentacao 5178
Assert-ProcessoApresentacao $api 'api'
Assert-ProcessoApresentacao $frontend 'frontend'
$logs = Join-Path $script:RaizApresentacao 'logs'
$null = New-Item -ItemType Directory -Path $logs -Force
$pgCtl = Join-Path $config.pgBin 'pg_ctl.exe'
& $pgCtl -D $config.dataDirectory status *> $null
if ($LASTEXITCODE -ne 0) {
    if (Get-ProcessoPortaApresentacao 55439) { throw 'Porta 55439 ocupada por outra instância; não será criada ou iniciada uma alternativa.' }
    & $pgCtl -D $config.dataDirectory -l (Join-Path $logs 'postgresql.log') -o '-h 127.0.0.1 -p 55439' -w -t 45 start
    if ($LASTEXITCODE -ne 0) { throw 'Instância persistente indisponível. Confira logs/postgresql.log. Nenhum banco alternativo foi criado.' }
}
$null = Get-IdentidadeBancoApresentacao $config
$env:DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao'
$env:DATABASE_USER = $config.user
# Mantém a autenticação PostgreSQL já configurada no cluster; não altera pg_hba ou senhas.
if (-not $env:DATABASE_PASSWORD) { $env:DATABASE_PASSWORD = '' }
$env:API_PROXY = 'http://127.0.0.1:18083'
if (-not $api) {
    $null = Start-Process -FilePath (Get-Command java.exe).Source -ArgumentList @(
        '-jar',('"'+$jar+'"'),'--server.port=18083','--server.address=127.0.0.1',
        '--spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao',
        '--debug=false','--logging.level.root=INFO','--logging.level.org.springframework=INFO'
    ) -WorkingDirectory (Join-Path $script:ProjetoApresentacao 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs 'api.log') -RedirectStandardError (Join-Path $logs 'api-erro.log')
}
Wait-CatalogoApresentacao 'http://127.0.0.1:18083/api/vagas'
if (-not $frontend) {
    $null = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c','npm.cmd run dev -- --host 127.0.0.1 --port 5178 --strictPort') -WorkingDirectory (Join-Path $script:ProjetoApresentacao 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs 'frontend.log') -RedirectStandardError (Join-Path $logs 'frontend-erro.log')
}
Wait-CatalogoApresentacao 'http://127.0.0.1:5178/api/vagas'
Write-Output 'Ambiente disponível: http://127.0.0.1:5178'
Write-Output 'Banco existente: cyberladies_apresentacao em 127.0.0.1:55439. Nenhuma conta ou credencial foi recriada.'
