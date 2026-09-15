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

    Xoa sach reports\allure-results truoc moi lan chay: Maven KHONG tu don thu muc nay, nen
    khong xoa thi ket qua cac lan chay truoc (ke ca lan bi loi luc debug) se bi GOP CHUNG vao
    report moi nhat - report se hien nham nhung dong da loi tu rat lau, gay hieu lam la loi
    hien tai. Xoa dam bao report luon chi phan anh dung LAN CHAY GAN NHAT.
#>
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\scripts\common.ps1"

Initialize-JimEnvironment

$results = Join-Path $PSScriptRoot 'reports\allure-results'
if (Test-Path $results) {
    Remove-Item $results -Recurse -Force
}

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
