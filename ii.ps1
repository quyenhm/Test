[CmdletBinding(DefaultParameterSetName = "Help")]
param(
    [ValidateSet("CORE", "AML", "SAFE", "BW", "IB", "DIGI", "SYS", "CLI", "LIC")]
    [Parameter(ParameterSetName = "Build")]
    [Parameter(ParameterSetName = "Test")]
    [Parameter(ParameterSetName = "Connection")]
    [Parameter(ParameterSetName = "Clean")]
    [string[]] $Projects = ("CORE", "AML", "SAFE", "BW", "IB", "DIGI", "SYS", "CLI", "LIC"),

    [Parameter(ParameterSetName = "Build")] [switch] $Build,
    [Parameter(ParameterSetName = "Build")] [switch] $IncludeTests,
    [Parameter(ParameterSetName = "Test")] [switch] $Test,
    [Parameter(ParameterSetName = "Test")] [switch] $ETR,
    [Parameter(ParameterSetName = "Test")] [switch] $SkipBuild,
    [Parameter(ParameterSetName = "Test")] [switch] $NoReport,

    [ValidateSet(
        "InstallCommandTests",
        "InstallCommandAppThenDbTests",
        "InstallCommandWithAccountTests",
        "InstallCommandUseExistingDbTests",
        "UpdateCommandTests",
        "UpdateCommandRollbackTests",
        "UpdateCommandIgnoreUpdateDbTests",
        "UpdateCommandBackupRestoreStateTests",
        "UninstallCommandTests",
        "UpdateCommandIgnoreUpdateDbTests",
        "ExportCommandTests"
    )]
    [Parameter(ParameterSetName = "Test")] [string] $Filter,

    [Parameter(ParameterSetName = "Test")] [string] $OutputPath,
    [Parameter(ParameterSetName = "Test")] [switch] $Summary,
    [Parameter(ParameterSetName = "Publish")] [switch] $Publish,
    [Parameter(ParameterSetName = "Publish")] [switch] $OpenOutput,
    [Parameter(ParameterSetName = "Connection")] [switch] $EditConn,
    [Parameter(ParameterSetName = "Connection")] [string] $SqlInfo = ".\SQL2022>>sa>>112233",
    [Parameter(ParameterSetName = "Clean")] [switch] $ResetConn,
    [Parameter(ParameterSetName = "Clean")] [switch] $RemoveBin,
    [Parameter(ParameterSetName = "Clean")] [switch] $RemoveTestUser,
    [Parameter(ParameterSetName = "Version", Mandatory)] [string] $AppVersion,
    [Parameter(ParameterSetName = "Version")] [string] $Suffix,
    [Parameter(ParameterSetName = "Help")] [switch] $Help
)

$ErrorActionPreference = "Stop"

$c = @{
    Hdr = $PSStyle.Foreground.Cyan
    Prm = $PSStyle.Foreground.Yellow
    Val = $PSStyle.Foreground.Blue
    Opt = $PSStyle.Foreground.White
    Dsc = $PSStyle.Foreground.White
    R   = $PSStyle.Reset
}

function Write-Param($color, $name, $value, $desc) { Write-Host "  $color$name$($c.R) $($c.Val)$value$($c.R) $($c.Dsc)$desc" }

if ($PSCmdlet.ParameterSetName -eq "Help") {
    Write-Host "ifsinstall (ii) - Utility for building, testing, and publishing."
    Write-Host "$($c.Hdr)PROJECTS ($($Projects.Count))"
    Write-Host "   $($Projects -join ", ")"
    Write-Host "$($c.Hdr)PARAMETERS"
    Write-Param $c.Prm '-Build         ' '         ' 'Build specified projects or entire solution'
    Write-Param $c.Opt '  -Projects    ' '<...>    ' 'Specify projects to build. Default is all projects'
    Write-Param $c.Opt '  -IncludeTests' '         ' 'Also build integration test projects'
    Write-Param $c.Prm '-Test          ' '         ' 'Build and run all tests'
    Write-Param $c.Prm '-ETR           ' '         ' 'Edit connection, run tests, then reset connection'
    Write-Param $c.Opt '  -Projects    ' '<...>    ' 'Specify projects to test. Default is all projects'
    Write-Param $c.Opt '  -SkipBuild   ' '         ' 'Skip rebuild before running'
    Write-Param $c.Opt '  -NoReport    ' '         ' 'Test report will not be shown'
    Write-Param $c.Opt '  -Filter      ' 'ClassName' 'Run only tests for specified filter'
    Write-Param $c.Opt '  -OutputPath  ' 'Path     ' 'Write test results to the specified path'
    Write-Param $c.Opt '  -Summary     ' '         ' 'Get summary of test results'
    Write-Param $c.Prm '-EditConn      ' '         ' 'Edit the connection string'
    Write-Param $c.Opt '  -SqlInfo     ' 'I>>U>>P  ' 'Specify the SQL Server instance, username and password'
    Write-Param $c.Opt '  -Projects    ' '<...>    ' 'Specify projects to test. Default is all projects'
    Write-Param $c.Prm '-ResetConn     ' '         ' 'Reset the connection string'
    Write-Param $c.Prm '-RemoveBin     ' '         ' 'Clean up all bin and obj folders'
    Write-Param $c.Prm '-RemoveTestUser' '         ' 'Clean up all test users created by integration tests'
    Write-Param $c.Prm '-AppVersion    ' 'x.y.z    ' 'Update version across all projects'
    Write-Param $c.Opt '  -Suffix      ' 'beta     ' 'Append a pre-release label (e.g. alpha, beta)'
    Write-Param $c.Prm '-Publish       ' '         ' 'Publish the ifsinstall CLI'
    Write-Param $c.Opt '  -OpenOutput  ' '         ' 'Open the publish folder when complete'
    return
}


switch ($PSBoundParameters.Keys) {
    "Build" {
        Write-Host -ForegroundColor Yellow "Build"
    }
    "Publish" {
        Write-Host -ForegroundColor Yellow "Publish"
    }
    "EditConn" {
        Write-Host -ForegroundColor Yellow "Edit Connection String"
    }
    "ResetConn" {
        Write-Host -ForegroundColor Yellow "Reset Connection String"
    }
    "RemoveBin" {
        Write-Host -ForegroundColor Yellow "Remove bin and obj folders"
    }
    "Test" {
        Write-Host -ForegroundColor Yellow "Run Test"
    }
    "ETR" {
    }
    "Summary" {
        $total = 0
        $passed = 0
        $failed = 0
        $skipped = 0

        $trxFiles = Get-ChildItem -Path $OutputPath -Filter *.trx -ErrorAction SilentlyContinue

        if (-not $trxFiles) {
            Write-Output "0,0,0,0"
            return
        }

        foreach ($file in $trxFiles) {
            [xml]$trx = Get-Content $file.FullName
            $counters = $trx.TestRun.ResultSummary.Counters

            $total += [int]$counters.total
            $passed += [int]$counters.passed
            $failed += [int]$counters.failed + [int]$counters.error
            $skipped += [int]$counters.notExecuted + [int]$counters.inconclusive
        }

        Write-Output "$total,$passed,$failed,$skipped"
    }
    "RemoveTestUser" {
        Write-Host -ForegroundColor Yellow "Remove Test User"
    }
    "AppVersion" {
        Write-Host -ForegroundColor Yellow "Update App Version"
    }
}
