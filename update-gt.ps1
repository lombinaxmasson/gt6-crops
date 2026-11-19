# Download the latest successful GT6CE NeoForge build and its matching API and SDK.
$ErrorActionPreference = 'Stop'
$repo = 'GregTech6CE/GregTech6'
$branch = gh repo view $repo --json defaultBranchRef --jq '.defaultBranchRef.name'
if ($LASTEXITCODE -ne 0 -or -not $branch) { throw 'Cannot determine the GT6CE default branch' }
$runJson = gh run list -R $repo --workflow 'Dual-platform build' --branch $branch --status success --limit 1 --json databaseId,headSha
if ($LASTEXITCODE -ne 0) { throw 'Cannot list GT6CE builds' }
$run = $runJson | ConvertFrom-Json | Select-Object -First 1
if (-not $run) { throw 'No successful GT6CE build found' }

$apiVersionPattern = '^gt6ce_api_version=(\S+)$'
$apiVersionSetting = Select-String -LiteralPath (Join-Path $PSScriptRoot 'gradle.properties') -Pattern $apiVersionPattern
if (-not $apiVersionSetting) { throw 'Set gt6ce_api_version in gradle.properties first' }
$apiVersion = $apiVersionSetting.Matches[0].Groups[1].Value
$dest = Join-Path $PSScriptRoot 'libs/gt6ce'
$staging = Join-Path $PSScriptRoot "build/gt6ce-download-$([guid]::NewGuid())"
New-Item -ItemType Directory -Force -Path $staging | Out-Null
$staging = (Resolve-Path -LiteralPath $staging).Path

function Get-BinaryJars($directory, $filter) {
    Get-ChildItem -LiteralPath $directory -Filter $filter -File | Where-Object {
        $_.Name -notmatch '-(sources|javadoc|dev)\.jar$'
    }
}

try {
    gh run download $run.databaseId -R $repo -n gt6ce-1.21.1-neoforge -D $staging
    if ($LASTEXITCODE -ne 0) { throw 'GT6CE artifact download failed' }

    $platform = Join-Path $staging 'minecraft/versions/1.21.1-neoforge/build'
    $api = @(Get-BinaryJars (Join-Path $staging 'api/common/build/libs') 'gt6ce-api-*.jar')
    $sdk = @(Get-BinaryJars (Join-Path $platform 'addon-sdk') 'gt6ce-addon-sdk-1.21.1-neoforge-*.jar')
    if ($api.Count -ne 1 -or $sdk.Count -ne 1 -or
        $api[0].Name -ne "gt6ce-api-$apiVersion.jar" -or
        $sdk[0].Name -ne "gt6ce-addon-sdk-1.21.1-neoforge-$apiVersion.jar") {
        throw "GT6CE build $($run.headSha) provides '$($api.Name)' and '$($sdk.Name)'. Review the API changes and update gt6ce_api_version in gradle.properties before replacing the pinned build."
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $mods = @(Get-BinaryJars (Join-Path $platform 'libs') '*.jar' | Where-Object {
        $archive = [IO.Compression.ZipFile]::OpenRead($_.FullName)
        try { $null -ne $archive.GetEntry('META-INF/neoforge.mods.toml') }
        finally { $archive.Dispose() }
    })
    if ($mods.Count -ne 1) { throw "Expected one GT6CE NeoForge mod jar, found $($mods.Count)" }

    $modName = 'gt6ce-1.21.1-neoforge.jar'
    $hash = (Get-FileHash -LiteralPath $mods[0].FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    Copy-Item -LiteralPath $api[0].FullName, $sdk[0].FullName -Destination $dest -Force
    Copy-Item -LiteralPath $mods[0].FullName -Destination (Join-Path $dest $modName) -Force
    Set-Content -LiteralPath (Join-Path $dest 'build-commit.txt') -Value $run.headSha -Encoding ascii -NoNewline
    Set-Content -LiteralPath (Join-Path $dest "$modName.sha256") -Value "$hash  $modName" -Encoding ascii
    Write-Host "GT6CE updated to $($run.headSha) (API and SDK $apiVersion)"
}
finally {
    $buildDir = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'build')).Path
    $downloadDir = (Resolve-Path -LiteralPath $staging).Path
    if ((Split-Path -Parent $downloadDir) -eq $buildDir -and
        (Split-Path -Leaf $downloadDir) -like 'gt6ce-download-*') {
        Remove-Item -LiteralPath $downloadDir -Recurse -Force
    }
}
