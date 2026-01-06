param(
    [switch] $EditConn,
    [switch] $Test,
    [switch] $Build,
    [switch] $Publish,
    [switch] $RemoveBin,
    [switch] $RemoveTestUser,
    [switch] $ResetConn,
    [switch] $NoPrompt,
    [switch] $TestNoBuild,
    [switch] $TestOutput,
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

    Write-Host -ForegroundColor Cyan "Retrieving current version tag in git"

    $gitTag = git describe --tags --exact-match 2>$null

    if ($gitTag) {
        $tagDate = git for-each-ref --format="%(creatordate:iso8601)" refs/tags/$gitTag
        $newTime = [DateTime]::Parse($tagDate).ToUniversalTime().Date
        Write-Host "Tag: $gitTag => Using timestamp: $newTime"

        New-Item -ItemType File -Path ".\file_$gitTag.txt" -Force -Value "Published with tag $gitTag on $newTime"
    }
    else {
        Write-Host "No git tag found for the current commit: $gitTag"
    }

    Write-Host "Project published successfully."
}

if ($RemoveBin) {
    Write-Host "Cleanup..."
    Write-Host "Remove bin and obj folders."
}

Write-Host "Done!"