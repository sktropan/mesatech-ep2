# Levanta BFF + MS en segundo plano (misma sesión PowerShell: variables de proceso).
# Requisitos: docker compose en infra/, JARs en infra/out/.
#
# Con Entra del equipo (recomendado):
#   $env:ENTRA_ISSUER_URI="https://login.microsoftonline.com/<TENANT>/v2.0"
#   $env:ENTRA_AUDIENCE="<API_CLIENT_ID>"
#   .\infra\scripts\run-local-stack.ps1
#
# Detener: Get-Job | Stop-Job; Get-Job | Remove-Job; Get-Process java -ErrorAction SilentlyContinue | Stop-Process

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$LogDir = Join-Path $RepoRoot "infra\out\logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (-not $env:ENTRA_ISSUER_URI) {
    Write-Warning "ENTRA_ISSUER_URI no definido — issuer common (solo arranque local)."
    $env:ENTRA_ISSUER_URI = "https://login.microsoftonline.com/common/v2.0"
}
if (-not $env:ENTRA_AUDIENCE) {
    Write-Warning "ENTRA_AUDIENCE no definido — placeholder."
    $env:ENTRA_AUDIENCE = "00000000-0000-0000-0000-000000000001"
}
if (-not $env:DB_USER) { $env:DB_USER = "mesatech" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "mesatech" }
if (-not $env:MS_SOLICITUDES_URL) { $env:MS_SOLICITUDES_URL = "http://localhost:8081" }
if (-not $env:MS_CATALOGO_URL) { $env:MS_CATALOGO_URL = "http://localhost:8082" }

function Start-MesatechJob {
    param([string]$Name, [string]$Jar, [string]$DbUrl)
    $jarPath = Join-Path $RepoRoot "infra\out\$Jar"
    if (-not (Test-Path $jarPath)) { throw "Falta $jarPath — ejecuta .\infra\scripts\build-jars.ps1" }
    $log = Join-Path $LogDir "$Name.log"
    $script = {
        param($Root, $JarPath, $LogPath, $EnvMap)
        Set-Location $Root
        foreach ($k in $EnvMap.Keys) { Set-Item -Path "Env:$k" -Value $EnvMap[$k] }
        & java -jar $JarPath *> $LogPath
    }
    $envMap = @{
        ENTRA_ISSUER_URI     = $env:ENTRA_ISSUER_URI
        ENTRA_AUDIENCE       = $env:ENTRA_AUDIENCE
        DB_USER              = $env:DB_USER
        DB_PASSWORD          = $env:DB_PASSWORD
        MS_SOLICITUDES_URL   = $env:MS_SOLICITUDES_URL
        MS_CATALOGO_URL      = $env:MS_CATALOGO_URL
        CORS_ALLOWED_ORIGINS = $env:CORS_ALLOWED_ORIGINS
    }
    # Cada MS tiene su base. No reutilizar un único DB_URL del proceso padre.
    if ($DbUrl) { $envMap["DB_URL"] = $DbUrl }
    $job = Start-Job -Name $Name -ScriptBlock $script -ArgumentList $RepoRoot, $jarPath, $log, $envMap
    Write-Host "Job $Name (Id $($job.Id)) — log: $log"
}

Start-MesatechJob "ms-solicitudes" "ms-solicitudes.jar" "jdbc:mysql://localhost:3306/solicitudes"
Start-Sleep -Seconds 12
Start-MesatechJob "ms-catalogo" "ms-catalogo.jar" "jdbc:mysql://localhost:3306/catalogo"
Start-Sleep -Seconds 12
Start-MesatechJob "bff" "bff.jar" ""
Write-Host "Espera ~20s. Prueba: curl.exe -i http://127.0.0.1:8080/public/hola"
Write-Host "Jobs: Get-Job | Format-Table"
