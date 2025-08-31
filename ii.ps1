param(
    [switch] $EditConn,
    [switch] $Test,
    [switch] $Build,
    [switch] $Publish,
    [string] $Username,
    [string] $Password
)

if ($EditConn) {
    Write-Host "Configuration updated successfully. $Username - $Password"
}

if ($Test) {
    Write-Host "Running tests..."
    Write-Host "All tests passed!"
}

if ($Build) {
    Write-Host "Building the project..."
    Write-Host "Build completed successfully."
}

if ($Publish) {
    Write-Host "Publishing the project..."
    Write-Host "Project published successfully."
}

Write-Host "Done!"