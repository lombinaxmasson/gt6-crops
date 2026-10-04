# Download the latest successful NeoForge build of GT6CE from SaltNya/GregTech6 into libs\gt6ce\
$repo = 'SaltNya/GregTech6'
$run = gh run list -R $repo --workflow build.yml --branch main --status success --limit 1 --json databaseId | ConvertFrom-Json
if (-not $run) { throw 'No successful GT6CE build found' }
Remove-Item -Recurse -Force libs\gt6ce -ErrorAction SilentlyContinue
gh run download $run[0].databaseId -R $repo -n gregtech6-1.21.1-neoforge -D libs\gt6ce
if ($LASTEXITCODE -ne 0) { throw 'Download failed' }
Write-Host "GT6CE updated to commit $(Get-Content libs\gt6ce\build-commit.txt)"