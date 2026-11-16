param([ValidateRange(1024,65535)][int]$PortaFrontend=5178,
      [ValidateRange(1024,65535)][int]$PortaApi=18083)
$ErrorActionPreference='Stop'
$projeto=Split-Path $PSScriptRoot -Parent
$backend=Join-Path $projeto 'backend'
$frontend=Join-Path $projeto 'frontend'
$artefato=Join-Path $backend 'target/cyberladies-0.0.1-SNAPSHOT.jar'
if(-not (Test-Path -LiteralPath $artefato)){throw 'Compile o backend com mvn package antes de iniciar.'}
$estadoPg=Join-Path $env:TEMP 'cyberladies-apresentacao-current.txt'
if(-not (Test-Path -LiteralPath $estadoPg)){throw 'Cluster isolado local não preparado. Consulte docs/INTEGRACAO_APRESENTACAO.md.'}
$cluster=(Get-Content -LiteralPath $estadoPg -Raw).Trim()
$prefixo=Join-Path $env:TEMP 'cyberladies-apresentacao-'
if(-not $cluster.StartsWith($prefixo,[StringComparison]::OrdinalIgnoreCase)){throw 'Caminho inesperado do cluster isolado.'}
$pgCtl='C:/Program Files/PostgreSQL/16/bin/pg_ctl.exe'
& $pgCtl -D $cluster status *> $null
if($LASTEXITCODE -ne 0){
    & $pgCtl -D $cluster -l (Join-Path $cluster 'server.log') -o '-h 127.0.0.1 -p 55439' -w start
    if($LASTEXITCODE -ne 0){throw 'Não foi possível iniciar o cluster isolado.'}
}
$env:DATABASE_URL='jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao'
$env:DATABASE_USER='migration_test'
# O cluster descartável usa trust somente em loopback; não usa senha do banco atual.
$env:DATABASE_PASSWORD=''
$env:API_PROXY="http://127.0.0.1:$PortaApi"
$api=Get-NetTCPConnection -State Listen -LocalPort $PortaApi -ErrorAction SilentlyContinue
if(-not $api){
    $processoApi=Start-Process -FilePath (Get-Command java.exe).Source -ArgumentList @('-jar',('"'+$artefato+'"'),"--server.port=$PortaApi",'--server.address=127.0.0.1','--debug=false','--logging.level.root=INFO','--logging.level.org.springframework=INFO') -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $backend 'target/apresentacao-api.log') -RedirectStandardError (Join-Path $backend 'target/apresentacao-api-erro.log')
    Write-Output "API iniciada (PID $($processoApi.Id))."
}else{
    $processoExistente=Get-CimInstance Win32_Process -Filter "ProcessId=$($api[0].OwningProcess)"
    if($processoExistente.CommandLine -notlike ('*'+$artefato+'*')){throw 'Porta da API ocupada por outro processo; nada foi encerrado.'}
}
$site=Get-NetTCPConnection -State Listen -LocalPort $PortaFrontend -ErrorAction SilentlyContinue
if(-not $site){
    $processoSite=Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c',"npm.cmd run dev -- --host 127.0.0.1 --port $PortaFrontend --strictPort") -WorkingDirectory $frontend -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $backend 'target/apresentacao-site.log') -RedirectStandardError (Join-Path $backend 'target/apresentacao-site-erro.log')
    Write-Output "Frontend iniciado (PID $($processoSite.Id))."
}else{Write-Output 'Porta do frontend já está em uso; confira a URL e os logs antes de reutilizar.'}
Write-Output "Endereço: http://127.0.0.1:$PortaFrontend"
Write-Output 'Banco separado: cyberladies_apresentacao em 55439. O banco da demonstração anterior não foi acessado.'
