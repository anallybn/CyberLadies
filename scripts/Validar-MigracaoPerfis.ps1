param(
 [Parameter(Mandatory=$true)][string]$Banco,
 [Parameter(Mandatory=$true)][string]$DiretorioRelatorio
)
$ErrorActionPreference='Stop'
if ($Banco -notmatch '^cyberladies_auth_[a-z0-9_]+$') { throw 'Somente banco isolado cyberladies_auth_* é permitido.' }
function AuthSql([string]$Consulta) {
 $AuthSaida = & docker compose exec -T banco psql -U cyberladies -d $Banco -qAt -v ON_ERROR_STOP=1 -c $Consulta
 if ($LASTEXITCODE -ne 0) { throw 'Falha na consulta do banco isolado.' }
 return $AuthSaida
}
if ((AuthSql "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='talk_usuaria' AND column_name='tipo'") -ne '0') {
 throw 'A cópia já foi migrada. Use uma restauração nova para comparar antes/depois.'
}
# Sessão sintética criada somente nesta cópia, antes de aplicar a migração.
$AuthLegacyEmail='fase1-legado-'+[Guid]::NewGuid().ToString('N')+'@example.invalid'
$AuthLegacyId=AuthSql "INSERT INTO talk_usuaria(nome,email,senha_hash,competencias) VALUES ('Fixture de sessão legada','$AuthLegacyEmail','fixture-preservada','Java') RETURNING id"
$AuthLegacyToken='0123456789abcdef'*4
$AuthHasher=[Security.Cryptography.SHA256]::Create()
try { $AuthLegacyHash=([BitConverter]::ToString($AuthHasher.ComputeHash([Text.Encoding]::ASCII.GetBytes($AuthLegacyToken)))).Replace('-','').ToLowerInvariant() }
finally { $AuthHasher.Dispose() }
AuthSql "INSERT INTO talk_sessao VALUES ('$AuthLegacyHash',$AuthLegacyId,CURRENT_TIMESTAMP+INTERVAL '8 hours')" | Out-Null

$AuthTabelas=@(AuthSql "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")
function Snapshot($Tabelas) {
 $AuthResultado=@{}
 foreach($AuthTabela in $Tabelas) {
  if($AuthTabela -notmatch '^[a-z_]+$') { throw 'Identificador inesperado.' }
  $AuthExpressao=if($AuthTabela -eq 'talk_usuaria') { "(to_jsonb(t)-'tipo')" } else { 'to_jsonb(t)' }
  $AuthResultado[$AuthTabela]=AuthSql "SELECT count(*)::text||':'||COALESCE(md5(string_agg(h,'' ORDER BY h)),'') FROM (SELECT md5($AuthExpressao::text) h FROM $AuthTabela t) s"
 }
 return $AuthResultado
}
$AuthAntes=Snapshot $AuthTabelas
$AuthSequencias=@(AuthSql "SELECT sequencename FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename")
$AuthSeqAntes=@{}
foreach($AuthSeq in $AuthSequencias) {
 if($AuthSeq -notmatch '^[a-z_]+$') { throw 'Sequência inesperada.' }
 $AuthSeqAntes[$AuthSeq]=AuthSql "SELECT last_value::text||':'||is_called::text FROM $AuthSeq"
}
$AuthMigration=Join-Path $PSScriptRoot '../backend/src/main/resources/db/auth-perfis-v1.sql'
& docker cp $AuthMigration 'cyberladies-banco-1:/tmp/auth-perfis-v1-validacao.sql'
if($LASTEXITCODE -ne 0) { throw 'Falha ao preparar a migração de teste.' }
& docker compose exec -T banco psql -U cyberladies -d $Banco -v ON_ERROR_STOP=1 --single-transaction -f /tmp/auth-perfis-v1-validacao.sql
if($LASTEXITCODE -ne 0) { throw 'Migração isolada falhou e foi revertida.' }

$AuthDepois=Snapshot $AuthTabelas
foreach($AuthTabela in $AuthTabelas) {
 if($AuthAntes[$AuthTabela] -ne $AuthDepois[$AuthTabela]) { throw "Dados divergentes: $AuthTabela" }
}
foreach($AuthSeq in $AuthSequencias) {
 if($AuthSeqAntes[$AuthSeq] -ne (AuthSql "SELECT last_value::text||':'||is_called::text FROM $AuthSeq")) { throw "Sequência divergente: $AuthSeq" }
}
if((AuthSql "SELECT count(*) FROM talk_usuaria WHERE tipo<>'CYBERLADIE'") -ne '0') { throw 'Tipo incorreto na migração de contas antigas.' }
[IO.Directory]::CreateDirectory($DiretorioRelatorio) | Out-Null
$AuthRelatorio=@{
 banco=$Banco
 tabelasPreservadas=$AuthTabelas.Count
 sequenciasPreservadas=$AuthSequencias.Count
 antes=$AuthAntes
 depois=$AuthDepois
 sessaoLegadaPreservada=((AuthSql "SELECT count(*) FROM talk_sessao WHERE token_hash='$AuthLegacyHash' AND usuaria_id=$AuthLegacyId") -eq '1')
 migrationSha256=(Get-FileHash -LiteralPath $AuthMigration).Hash
}
$AuthRelatorio | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $DiretorioRelatorio ($Banco+'-compatibilidade.json')) -Encoding utf8
Write-Output "PASS: $($AuthTabelas.Count) tabelas, $($AuthSequencias.Count) sequências e sessão legada preservadas em $Banco."