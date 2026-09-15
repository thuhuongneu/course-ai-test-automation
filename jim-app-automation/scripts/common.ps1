# Ham dung chung cho run-tests.ps1 va show-report.ps1.
# Tach rieng de hai script khong lap lai logic tim JDK / Maven.

$script:ProjectRoot = Split-Path -Parent $PSScriptRoot

function Get-JimMaven {
    <#  Maven dong san trong repo - khong bat nguoi dung cai Maven len may.  #>
    $mvn = Join-Path $script:ProjectRoot '..\selenium-test-automation\.maven-local\apache-maven-3.9.9\bin\mvn.cmd'
    $mvn = [System.IO.Path]::GetFullPath($mvn)
    if (-not (Test-Path $mvn)) {
        throw "Khong tim thay Maven tai: $mvn`nRepo co con thu muc selenium-test-automation\.maven-local khong?"
    }
    return $mvn
}

function Initialize-JimEnvironment {
    <#
        Dat JAVA_HOME cho phien PowerShell hien tai.
        Uu tien JAVA_HOME san co neu no hop le; neu khong thi tu do JDK trong may.
    #>
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        Write-Host "JAVA_HOME  : $env:JAVA_HOME (dang co san)" -ForegroundColor DarkGray
        return
    }

    $candidates = @()
    foreach ($base in @("$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium")) {
        if (Test-Path $base) {
            $candidates += Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') }
        }
    }

    if (-not $candidates) {
        throw "Khong tim thay JDK tren may. Hay cai JDK 11 tro len, hoac dat JAVA_HOME thu cong."
    }

    # Ban moi nhat theo ten thu muc
    $jdk = ($candidates | Sort-Object Name -Descending)[0].FullName
    $env:JAVA_HOME = $jdk
    Write-Host "JAVA_HOME  : $jdk (tu do duoc)" -ForegroundColor DarkGray
}
