# ========================================================
# HopTell — Google Play Release Bundle (.aab) Builder
# ========================================================

$ErrorActionPreference = "Continue"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  HopTell (Google Play Release AAB Builder)      " -ForegroundColor Yellow
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Web resurslarni sinxronizatsiya qilish
Write-Host "`n[1/4] Web resurslar Android loyihasiga sinxronlanmoqda..." -ForegroundColor Cyan
& npx.cmd cap sync android

if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] Sinxronizatsiyada xatolik yuz berdi." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Sinxronizatsiya muvaffaqiyatli yakunlandi." -ForegroundColor Green

# 2. Java / JDK va Android SDK ni sozlash
Write-Host "`n[2/4] Tizimda Java (JDK 21) va Android SDK tekshirilmoqda..." -ForegroundColor Cyan

$Jdk21 = "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot"
$Jdk17 = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"

if (Test-Path $Jdk21) {
    $env:JAVA_HOME = $Jdk21
    $env:PATH = "$Jdk21\bin;$env:PATH"
    Write-Host "[OK] Microsoft OpenJDK 21 ulandi: $Jdk21" -ForegroundColor Green
} elseif (Test-Path $Jdk17) {
    $env:JAVA_HOME = $Jdk17
    $env:PATH = "$Jdk17\bin;$env:PATH"
    Write-Host "[OK] Microsoft OpenJDK 17 ulandi: $Jdk17" -ForegroundColor Green
}

$AndroidSdk = "D:\Android\sdk"
if (Test-Path $AndroidSdk) {
    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    $env:PATH = "$AndroidSdk\platform-tools;$AndroidSdk\cmdline-tools\latest\bin;$env:PATH"
    Write-Host "[OK] Android SDK ulandi: $AndroidSdk" -ForegroundColor Green
}

$JavaCheck = Get-Command java -ErrorAction SilentlyContinue

if ($JavaCheck) {
    Write-Host "`n[3/4] Gradle orqali Imzolangan Release AAB yig'ilmoqda (bundleRelease)..." -ForegroundColor Cyan
    
    $AndroidDir = Join-Path $PSScriptRoot "android"
    Push-Location $AndroidDir
    
    & .\gradlew.bat bundleRelease
    $BuildExitCode = $LASTEXITCODE
    
    Pop-Location

    if ($BuildExitCode -eq 0) {
        $SourceAab = Join-Path $PSScriptRoot "android\app\build\outputs\bundle\release\app-release.aab"
        $TargetAab = Join-Path $PSScriptRoot "HopTell.aab"
        Copy-Item -Path $SourceAab -Destination $TargetAab -Force
        
        Write-Host "`n[4/4] [TABRIKLAYMIZ!] HopTell Release AAB muvaffaqiyatli tayyorlandi!" -ForegroundColor Green
        Write-Host "Play Store AAB fayl: $TargetAab" -ForegroundColor Yellow
        Write-Host "Hajmi:               $((Get-Item $TargetAab).Length / 1MB | ForEach-Object { '{0:N2} MB' -f $_ })" -ForegroundColor Cyan
    } else {
        Write-Host "`n[X] Gradle orqali yig'ishda xatolik yuz berdi (Exit code: $BuildExitCode)." -ForegroundColor Red
        exit $BuildExitCode
    }
} else {
    Write-Host "[!] Java topilmadi. Iltimos, Java 21 o'rnatilganligini tekshiring." -ForegroundColor Red
    exit 1
}
