param(
    [switch] $EditConn,
    [switch] $Test,
    [switch] $Build,
    [switch] $Publish,
    [switch] $RemoveBin,
    [string] $Projects,
    [string] $Username,
    [string] $Password,
    [int] $ExitCode,
    [switch] $Throw
)

if ($EditConn) {
    Write-Host "Configuration updated successfully. $Username - $Password"
}

if ($Test) {
    Write-Host "Running tests $Projects..."
    Write-Host "Stop script by 'exit $ExitCode'"
    exit $ExitCode
    Write-Host "All tests passed!"
}

if ($Throw) {
    Write-Host "Pwsh throw test"
    throw "Simulated error for testing purposes."
}

if ($Build) {
    Write-Host "Building the project..."
    Write-Host "Stop script by 'return'"
    return
    Write-Host "Build completed successfully."
}

if ($Publish) {
    Write-Host "Publishing the project..."
    Write-Host "v2.0.0"
    Write-Host "Project published successfully."
}

if ($RemoveBin) {
    Write-Host "Cleanup..."
    Write-Host "Remove bin and obj folders."
}

Write-Host "Done!"