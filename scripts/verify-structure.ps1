$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
[xml](Get-Content -Raw (Join-Path $root 'pom.xml')) | Out-Null
$required = @(
  'Dockerfile','compose.yml','README.md','.github/workflows/ci.yml',
  'src/main/resources/application.yml','src/main/resources/db/migration/V1__init.sql',
  'k8s/base/deployment.yaml','k8s/base/kustomization.yaml'
)
foreach ($file in $required) {
  if (-not (Test-Path (Join-Path $root $file))) { throw "Missing required file: $file" }
}
$java = Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java'
if ($java.Count -lt 15) { throw "Unexpected Java source count: $($java.Count)" }
Write-Host "Structure verification passed: $($java.Count) Java files; pom.xml is well-formed."

