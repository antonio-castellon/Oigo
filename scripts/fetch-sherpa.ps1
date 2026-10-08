# Downloads sherpa-onnx 1.13.8 Android JNI libs (Apache-2.0) once.
# Only arm64-v8a and armeabi-v7a, and only the two libraries the spotter loads.
$ErrorActionPreference = "Stop"
$proj = Split-Path $PSScriptRoot -Parent
$jni = Join-Path $proj "app\src\main\jniLibs"
$need = @(
    "arm64-v8a\libsherpa-onnx-jni.so",
    "arm64-v8a\libonnxruntime.so",
    "armeabi-v7a\libsherpa-onnx-jni.so",
    "armeabi-v7a\libonnxruntime.so"
)
$missing = @($need | Where-Object { -not (Test-Path (Join-Path $jni $_)) })
if ($missing.Count -eq 0) {
    Write-Output "sherpa jni already present"
    exit 0
}

$cache = Join-Path $env:LOCALAPPDATA "GrokAndroid"
New-Item -ItemType Directory -Force -Path $cache | Out-Null
$tar = Join-Path $cache "sherpa-onnx-v1.13.8-android.tar.bz2"
$url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2"
if (-not (Test-Path $tar) -or (Get-Item $tar).Length -lt 1000000) {
    Write-Output "downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $tar
}
$unpack = Join-Path $cache "sherpa-android-v1.13.8"
if (Test-Path $unpack) { Remove-Item $unpack -Recurse -Force }
New-Item -ItemType Directory -Force -Path $unpack | Out-Null
Write-Output "extracting sherpa android libs"
& python (Join-Path $PSScriptRoot "extract-sherpa.py") $tar $unpack
$libs = Get-ChildItem $unpack -Recurse -Filter "libsherpa-onnx-jni.so"
if (-not $libs) { throw "libsherpa-onnx-jni.so was not in the archive" }
foreach ($lib in $libs) {
    $abi = $lib.Directory.Name
    if ($abi -ne "arm64-v8a" -and $abi -ne "armeabi-v7a") { continue }
    $dest = Join-Path $jni $abi
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    Copy-Item $lib.FullName (Join-Path $dest $lib.Name) -Force
    $ort = Join-Path $lib.DirectoryName "libonnxruntime.so"
    if (Test-Path $ort) {
        Copy-Item $ort (Join-Path $dest "libonnxruntime.so") -Force
    }
}
foreach ($rel in $need) {
    if (-not (Test-Path (Join-Path $jni $rel))) { throw "missing $rel" }
}
Write-Output "sherpa jni ready"
