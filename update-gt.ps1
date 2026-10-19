# Download the latest successful NeoForge build of GT6CE from SaltNya/GregTech6 into libs\gt6ce\
# The artifact contains the mod jar and the addon SDK. Compilation uses only the SDK.
$repo = 'SaltNya/GregTech6'
$run = gh run list -R $repo --workflow build.yml --branch main --status success --limit 1 --json databaseId | ConvertFrom-Json
if (-not $run) { throw 'No successful GT6CE build found' }
Remove-Item -Recurse -Force libs\gt6ce -ErrorAction SilentlyContinue
gh run download $run[0].databaseId -R $repo -n gregtech6-1.21.1-neoforge -D libs\gt6ce
if ($LASTEXITCODE -ne 0) { throw 'Download failed' }
$mod = Get-ChildItem libs\gt6ce\gregtech6-1.21.1-neoforge.jar -ErrorAction SilentlyContinue
$sdk = Get-ChildItem libs\gt6ce\gregtech6-addon-sdk-1.21.1-neoforge-*.jar -ErrorAction SilentlyContinue
if (-not $mod -or -not $sdk) { throw 'The GT6CE artifact must contain the NeoForge mod jar and the addon SDK' }
Write-Host "GT6CE updated to commit $(Get-Content libs\gt6ce\build-commit.txt)"
Write-Host "SDK: $($sdk.Name)"
