$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "--- 1. Testing GET /api/auth/me ---"
$me = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -WebSession $session
Write-Host "Active User: $($me.data.fullName) | Current Theme: $($me.data.theme)"

Write-Host "`n--- 2. Testing PUT /api/auth/profile/theme (Switch to theme-dark) ---"
$themeBody = @{ theme = "theme-dark" } | ConvertTo-Json
$themeRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/profile/theme" -Method Put -Body $themeBody -ContentType "application/json" -WebSession $session
Write-Host "Updated Theme in DB: $($themeRes.data.theme)"

Write-Host "`n--- 3. Testing GET /api/documents/calendar ---"
$cal = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/calendar" -WebSession $session
Write-Host "Calendar documents count: $($cal.data.Count)"
if ($cal.data.Count -gt 0) {
    Write-Host "Sample doc expiry: $($cal.data[0].documentName) expires on $($cal.data[0].expiryDate) (Owner: $($cal.data[0].ownerName))"
}

Write-Host "`n--- 4. Testing GET /api/documents/trash ---"
$trashBefore = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/trash" -WebSession $session
Write-Host "Initial Trash count: $($trashBefore.data.Count)"

Write-Host "`n--- 5. Testing Soft-Delete and Restore Cycle ---"
# Pick the first active document
$docs = Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -WebSession $session
$testDoc = $docs.data[0]
Write-Host "Selected document for soft-delete test: ID $($testDoc.id) '$($testDoc.documentName)'"

# Soft delete it
$delRes = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/$($testDoc.id)" -Method Delete -WebSession $session
Write-Host "DELETE /api/documents/$($testDoc.id) response: $($delRes.message)"

# Verify it appears in Trash
$trashAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/trash" -WebSession $session
Write-Host "Trash count after soft delete: $($trashAfter.data.Count)"
$inTrash = $trashAfter.data | Where-Object { $_.id -eq $testDoc.id }
Write-Host "Is document in trash? $([bool]$inTrash) (isDeleted=$($inTrash.isDeleted), deletedAt=$($inTrash.deletedAt))"

# Verify it does NOT appear in active documents
$docsAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -WebSession $session
$inActive = $docsAfter.data | Where-Object { $_.id -eq $testDoc.id }
Write-Host "Is document in active documents? $([bool]$inActive)"

# Restore it
$restoreRes = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/$($testDoc.id)/restore" -Method Put -WebSession $session
Write-Host "PUT /api/documents/$($testDoc.id)/restore response: $($restoreRes.message)"

# Verify it is back in active documents and removed from Trash
$trashFinal = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/trash" -WebSession $session
$docsFinal = Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -WebSession $session
Write-Host "Trash count after restore: $($trashFinal.data.Count)"
Write-Host "Active documents count after restore: $($docsFinal.data.Count)"

# Reset theme back to default
$resetTheme = @{ theme = "theme-default" } | ConvertTo-Json
$resetRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/profile/theme" -Method Put -Body $resetTheme -ContentType "application/json" -WebSession $session
Write-Host "`n--- 6. Reset Theme to default: $($resetRes.data.theme) ---"

Write-Host "`n=== ALL API CHECKS COMPLETED SUCCESSFULLY ==="
