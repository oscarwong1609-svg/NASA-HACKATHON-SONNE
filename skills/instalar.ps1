# Instala las skills de esta carpeta en la carpeta personal de Claude Code (~/.claude/skills).
# Uso, desde la raiz del repositorio:  powershell -ExecutionPolicy Bypass -File skills\instalar.ps1
$ErrorActionPreference = 'Stop'
$origen = $PSScriptRoot
$destino = Join-Path $HOME '.claude\skills'
New-Item -ItemType Directory -Force -Path $destino | Out-Null

$n = 0
Get-ChildItem -Path $origen -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'SKILL.md') } | ForEach-Object {
    $objetivo = Join-Path $destino $_.Name
    New-Item -ItemType Directory -Force -Path $objetivo | Out-Null
    Copy-Item -Path (Join-Path $_.FullName '*') -Destination $objetivo -Recurse -Force
    Write-Host "Instalada: $($_.Name)"
    $n++
}
Write-Host ""
Write-Host "$n skills instaladas en $destino"
Write-Host "Cierra y vuelve a abrir Claude Code para que las cargue. Los plugins se instalan aparte (ver skills\README.md)."
