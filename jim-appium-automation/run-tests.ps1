<#
.SYNOPSIS
    Chay automation test cho app JIM POS desktop.

.DESCRIPTION
    Tu lo JAVA_HOME va duong dan Maven dong san trong repo, nen chi can chay:
        .\run-tests.ps1
    Moi tham so them vao se duoc chuyen thang cho Maven, vi du chay mot test le:
        .\run-tests.ps1 -Dtest=LoginTest#login_validCredentials_opensSelectRegister -DfailIfNoSpecifiedTests=false

.NOTES
    May KHONG duoc khoa man hinh va dung cham chuot/ban phim trong luc chay (~40 giay):
    test dieu khien chuot va ban phim that, app phai o foreground.
#>
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\scripts\common.ps1"

Initialize-JimEnvironment

Write-Host ""
Write-Host "Dang chay test... may khong duoc khoa man hinh, dung cham chuot/ban phim (~40 giay)." -ForegroundColor Yellow
Write-Host ""

# Maven chay theo thu muc hien tai -> phai dung o goc project du script duoc goi tu dau
Push-Location $PSScriptRoot
try {
    & (Get-JimMaven) '-B' 'test' @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
