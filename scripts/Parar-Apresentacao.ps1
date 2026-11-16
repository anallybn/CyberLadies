. (Join-Path $PSScriptRoot 'Apresentacao-Comum.ps1')
$config = Get-AmbienteApresentacao
$api = Get-ProcessoPortaApresentacao 18083
$frontend = Get-ProcessoPortaApresentacao 5178
Assert-ProcessoApresentacao $api 'api'
Assert-ProcessoApresentacao $frontend 'frontend'
# Valida as duas portas antes de encerrar qualquer processo.
if ($frontend) { Stop-Process -Id $frontend.ProcessId; Write-Output 'Frontend encerrado.' }
if ($api) { Stop-Process -Id $api.ProcessId; Write-Output 'API encerrada.' }
$pgCtl = Join-Path $config.pgBin 'pg_ctl.exe'
& $pgCtl -D $config.dataDirectory status *> $null
if ($LASTEXITCODE -eq 0) {
    $null = Get-IdentidadeBancoApresentacao $config
    & $pgCtl -D $config.dataDirectory -m fast -w -t 45 stop
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL não confirmou o encerramento. Não foi usado modo immediate nem removidos arquivos.' }
} elseif (Get-ProcessoPortaApresentacao 55439) {
    throw 'A porta 55439 pertence a outra instância; ela não foi encerrada.'
}
Write-Output 'Ambiente parado. Banco, contas, credenciais, papéis e demais dados continuam no armazenamento persistente.'
