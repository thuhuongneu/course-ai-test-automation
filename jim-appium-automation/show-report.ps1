<#
.SYNOPSIS
    Sinh va mo report Allure cua lan chay gan nhat.

.DESCRIPTION
        .\show-report.ps1            # sinh report roi tu mo trinh duyet
        .\show-report.ps1 -NoOpen    # chi sinh ra dia (reports\allure-report), khong mo trinh duyet

    Allure CLI duoc giai nen san vao .allure\ tu kho .m2 nen KHONG can mang va
    KHONG can cai Allure len may.
#>
param(
    [switch]$NoOpen
)
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\scripts\common.ps1"

Initialize-JimEnvironment

$results = Join-Path $PSScriptRoot 'reports\allure-results'
if (-not (Test-Path $results) -or -not (Get-ChildItem $results -Filter '*-result.json' -ErrorAction SilentlyContinue)) {
    throw "Chua co ket qua test trong reports\allure-results - hay chay .\run-tests.ps1 truoc."
}

$goal = if ($NoOpen) { 'allure:report' } else { 'allure:serve' }

# Maven chay theo thu muc hien tai -> phai dung o goc project du script duoc goi tu dau
Push-Location $PSScriptRoot
try {
    & (Get-JimMaven) '-B' $goal
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
