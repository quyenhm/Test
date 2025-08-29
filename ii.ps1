param(
    [switch] $EditConn,
    [string] $Username,
    [string] $Password
)

if ($EditConn) {
    Write-Host "Configuration updated successfully. $Username - $Password"
}
else {
    Write-Host "No changes made to configuration."
}

Write-Host "Done!"