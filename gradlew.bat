@echo off
setlocal

set SCRIPT_DIR=%~dp0
set WRAPPER_PROPS=%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.properties

if not exist "%WRAPPER_PROPS%" (
    echo Missing gradle wrapper properties at %WRAPPER_PROPS%>&2
    exit /b 1
)

for /f "usebackq tokens=*" %%I in (`powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$scriptDir = Resolve-Path '%SCRIPT_DIR%';" ^
  "$wrapper = Join-Path $scriptDir 'gradle/wrapper/gradle-wrapper.properties';" ^
  "$line = Get-Content $wrapper | Where-Object { $_ -like 'distributionUrl=*' };" ^
  "if (-not $line) { Write-Error 'distributionUrl not found.'; exit 1 }" ^
  "$url = ($line -split '=',2)[1].Replace('\:',':');" ^
  "$archiveName = Split-Path $url -Leaf;" ^
  "$distDir = Join-Path $scriptDir '.gradle-wrapper';" ^
  "$archivePath = Join-Path $distDir $archiveName;" ^
  "$gradleDirName = $archiveName.Substring(0, $archiveName.Length - 4);" ^
  "$gradleDirName = $gradleDirName -replace '-bin$', '' -replace '-all$', '';" ^
  "$gradleHome = Join-Path $distDir $gradleDirName;" ^
  "if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }" ^
  "if (-not (Test-Path $gradleHome)) {" ^
  "  if (-not (Test-Path $archivePath)) {" ^
  "    Write-Host ('Downloading Gradle distribution ' + $archiveName) 1>&2;" ^
  "    if (Get-Command Invoke-WebRequest -ErrorAction SilentlyContinue) {" ^
  "      Invoke-WebRequest -Uri $url -OutFile $archivePath" ^
  "    } elseif (Get-Command Start-BitsTransfer -ErrorAction SilentlyContinue) {" ^
  "      Start-BitsTransfer -Source $url -Destination $archivePath" ^
  "    } else {" ^
  "      $client = New-Object System.Net.WebClient;" ^
  "      $client.DownloadFile($url, $archivePath)" ^
  "    }" ^
  "  }" ^
  "  Write-Host 'Extracting Gradle distribution...' 1>&2;" ^
  "  if (Get-Command Expand-Archive -ErrorAction SilentlyContinue) {" ^
  "    Expand-Archive -Path $archivePath -DestinationPath $distDir -Force" ^
  "  } else {" ^
  "    Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
  "    [System.IO.Compression.ZipFile]::ExtractToDirectory($archivePath, $distDir)" ^
  "  }" ^
  "}" ^
  "$gradleBat = Join-Path $gradleHome 'bin/gradle.bat';" ^
  "if (-not (Test-Path $gradleBat)) { $gradleBat = Join-Path $gradleHome 'bin/gradle' }" ^
  "Write-Output $gradleBat"`) do set GRADLE_CMD=%%I

if not defined GRADLE_CMD (
    echo Failed to bootstrap Gradle distribution.>&2
    exit /b 1
)

"%GRADLE_CMD%" %*
