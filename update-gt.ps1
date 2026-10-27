# Download the latest successful NeoForge build of GT6CE from SaltNya/GregTech6 into libs\gt6ce\
# Compilation needs the common API jar and the NeoForge addon SDK. The mod jar is runtime only.
$repo = 'SaltNya/GregTech6'
$run = gh run list -R $repo --workflow build.yml --branch main --status success --limit 1 --json databaseId,headSha | ConvertFrom-Json
if (-not $run) { throw 'No successful GT6CE build found' }
$dest = 'libs\gt6ce'
$staging = Join-Path $dest 'download'
Remove-Item -Recurse -Force $staging -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $staging | Out-Null
gh run download $run[0].databaseId -R $repo -n gt6ce-1.21.1-neoforge -D $staging
if ($LASTEXITCODE -ne 0) { throw 'Download failed' }

function Take-BinaryJar($directory, $filter) {
    $jars = @(Get-ChildItem $directory -Filter $filter -ErrorAction SilentlyContinue | Where-Object {
        $_.Name -notmatch '-(sources|javadoc)\.jar$' -and $_.Name -notmatch '-dev\.jar$'
    })
    if ($jars.Count -ne 1) { throw "Expected one $filter in $directory, found $($jars.Count)" }
    return $jars[0]
}

$api = Take-BinaryJar (Join-Path $staging 'api\common\build\libs') 'gt6ce-api-*.jar'
$sdk = Take-BinaryJar (Join-Path $staging 'minecraft\versions\1.21.1-neoforge\build\addon-sdk') 'gt6ce-addon-sdk-1.21.1-neoforge-*.jar'
$mods = @(Get-ChildItem (Join-Path $staging 'minecraft\versions\1.21.1-neoforge\build\libs') -Filter '*.jar' | Where-Object {
    $_.Name -notmatch '-(sources|javadoc)\.jar$' -and $_.Name -notmatch '-dev\.jar$'
})
$mod = $mods | Where-Object {
    $listed = & jar tf $_.FullName
    $listed -contains 'META-INF/neoforge.mods.toml'
} | Select-Object -First 1
if (-not $mod) { throw 'The GT6CE artifact has no NeoForge mod jar' }

Copy-Item $api.FullName (Join-Path $dest $api.Name) -Force
Copy-Item $sdk.FullName (Join-Path $dest $sdk.Name) -Force
Copy-Item $mod.FullName (Join-Path $dest 'gt6ce-1.21.1-neoforge.jar') -Force
Remove-Item -Recurse -Force $staging
Set-Content -Path (Join-Path $dest 'build-commit.txt') -Value $run[0].headSha -NoNewline
Write-Host "GT6CE updated to commit $($run[0].headSha)"
Write-Host "API: $($api.Name)"
Write-Host "SDK: $($sdk.Name)"
