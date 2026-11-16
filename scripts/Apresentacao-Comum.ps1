$ErrorActionPreference = 'Stop'
$script:ProjetoApresentacao = Split-Path $PSScriptRoot -Parent
$script:RaizApresentacao = Join-Path $env:LOCALAPPDATA 'CyberLadies/apresentacao'
$script:ConfiguracaoApresentacao = Join-Path $script:RaizApresentacao 'ambiente.json'

function Get-AmbienteApresentacao {
    if (-not (Test-Path -LiteralPath $script:ConfiguracaoApresentacao -PathType Leaf)) {
        throw 'Configuração persistente ausente. O banco existente precisa ser localizado; nenhum banco será criado automaticamente.'
    }
    $config = Get-Content -LiteralPath $script:ConfiguracaoApresentacao -Raw | ConvertFrom-Json
    if ($config.database -ne 'cyberladies_apresentacao' -or $config.port -ne 55439 -or
        $config.apiPort -ne 18083 -or $config.frontendPort -ne 5178) {
        throw 'Configuração diferente do ambiente fixo esperado. Nenhum serviço foi substituído.'
    }
    $esperado = [IO.Path]::GetFullPath((Join-Path $script:RaizApresentacao 'postgresql/data'))
    if ([IO.Path]::GetFullPath($config.dataDirectory) -ne $esperado -or
        -not (Test-Path -LiteralPath (Join-Path $esperado 'PG_VERSION') -PathType Leaf)) {
        throw 'Armazenamento persistente existente não encontrado. Não será executado initdb, restore ou createdb.'
    }
    if ((Get-Content -LiteralPath (Join-Path $esperado 'PG_VERSION') -Raw).Trim() -ne '16') {
        throw 'Versão do cluster inesperada; não será feita atualização automática.'
    }
    foreach ($binario in @('pg_ctl.exe','psql.exe')) {
        if (-not (Test-Path -LiteralPath (Join-Path $config.pgBin $binario))) { throw "Executável PostgreSQL ausente: $binario" }
    }
    return $config
}

function Get-ProcessoPortaApresentacao([int]$Porta) {
    $listeners = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object LocalPort -eq $Porta)
    $ids = @($listeners.OwningProcess | Sort-Object -Unique)
    if ($ids.Count -gt 1) { throw "Mais de um processo está usando a porta $Porta." }
    if ($ids.Count -eq 1) { return Get-CimInstance Win32_Process -Filter "ProcessId=$($ids[0])" }
    return $null
}

function Assert-ProcessoApresentacao($Processo, [ValidateSet('api','frontend')]$Tipo) {
    if (-not $Processo) { return }
    $linha = $Processo.CommandLine
    if ($Tipo -eq 'api') {
        $jar = Join-Path $script:ProjetoApresentacao 'backend/target/cyberladies-0.0.1-SNAPSHOT.jar'
        $valido = $Processo.Name -eq 'java.exe' -and $linha -and $linha.Contains($jar) -and
            $linha.Contains('--server.port=18083') -and
            $linha.Contains('--spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao')
    } else {
        $pasta = Join-Path $script:ProjetoApresentacao 'frontend'
        $valido = $Processo.Name -eq 'node.exe' -and $linha -and $linha.Contains($pasta) -and
            $linha -match 'vite' -and $linha -match '--port\s+5178(?:\s|$)'
    }
    if (-not $valido) { throw "Porta do $Tipo ocupada por processo que não corresponde a este ambiente. Nada foi encerrado." }
}

function Get-IdentidadeBancoApresentacao($Config) {
    $consulta = "SELECT json_build_object('directory',current_setting('data_directory'),'systemIdentifier',(SELECT system_identifier::text FROM pg_control_system()),'databaseOid',(SELECT oid::text FROM pg_database WHERE datname=current_database()))::text;"
    $saida = & (Join-Path $Config.pgBin 'psql.exe') -X -w -q -t -A -h 127.0.0.1 -p 55439 -U $Config.user -d cyberladies_apresentacao -v ON_ERROR_STOP=1 -c $consulta 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'O banco cyberladies_apresentacao está indisponível ou o acesso foi recusado. Nenhum banco ou conta foi criado.' }
    try { $identidade = ($saida -join "`n") | ConvertFrom-Json } catch { throw 'Não foi possível confirmar a identidade do banco existente.' }
    if ([IO.Path]::GetFullPath($identidade.directory) -ne [IO.Path]::GetFullPath($Config.dataDirectory) -or
        $identidade.systemIdentifier -ne $Config.systemIdentifier -or $identidade.databaseOid -ne $Config.databaseOid) {
        throw 'A instância em 55439 não é o armazenamento cadastrado. Não será reutilizada ou substituída.'
    }
    return $identidade
}

function Wait-CatalogoApresentacao([string]$Url) {
    $limite = [DateTime]::UtcNow.AddSeconds(45)
    do {
        try {
            $resposta = Invoke-RestMethod -Uri $Url -TimeoutSec 2
            if ($resposta.PSObject.Properties.Name -contains 'vagas') { return }
        } catch { }
        Start-Sleep -Milliseconds 300
    } while ([DateTime]::UtcNow -lt $limite)
    throw "O serviço não ficou disponível: $Url. Confira os logs persistentes; nenhum banco alternativo foi criado."
}
