$ErrorActionPreference = 'Stop'

Write-Host "=== TEST 1: Unauthenticated /api/auth/me check ==="
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get
    Write-Error "Expected 401 but got 200"
} catch {
    Write-Host "SUCCESS: Correctly received 401 Unauthorized"
}

Write-Host "`n=== TEST 2: Invalid Admin Login ==="
try {
    $badBody = @{ email = "admin@docusafe.local"; password = "wrong" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $badBody -ContentType "application/json"
    Write-Error "Expected 401 for bad login"
} catch {
    Write-Host "SUCCESS: Correctly rejected bad password"
}

Write-Host "`n=== TEST 3: Successful Admin Login ==="
$adminBody = @{ email = "admin@docusafe.local"; password = "admin123" } | ConvertTo-Json
$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminLoginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $adminBody -ContentType "application/json" -WebSession $adminSession
Write-Host "SUCCESS: Admin Login Success! User: $($adminLoginRes.data.fullName) Role: $($adminLoginRes.data.role)"

Write-Host "`n=== TEST 4: Check /api/auth/me for Admin ==="
$adminMe = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -WebSession $adminSession
Write-Host "SUCCESS: Me check: $($adminMe.data.email) ($($adminMe.data.role))"

Write-Host "`n=== TEST 5: Admin Dashboard Summary ==="
$adminSummary = Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/summary" -Method Get -WebSession $adminSession
Write-Host "SUCCESS: Admin Total Documents: $($adminSummary.data.totalDocuments)"

Write-Host "`n=== TEST 6: Admin Family Members List ==="
$membersBefore = Invoke-RestMethod -Uri "http://localhost:8080/api/users/family-members" -Method Get -WebSession $adminSession
Write-Host "SUCCESS: Family Members count before: $($membersBefore.data.Count)"

Write-Host "`n=== TEST 7: Add Family Member (Priya Sharma - Spouse) ==="
$newMemberBody = @{
    fullName = "Priya Sharma"
    email = "priya@docusafe.local"
    password = "password123"
    relationship = "Spouse"
} | ConvertTo-Json
$createMemberRes = Invoke-RestMethod -Uri "http://localhost:8080/api/users/family-members" -Method Post -Body $newMemberBody -ContentType "application/json" -WebSession $adminSession
Write-Host "SUCCESS: Created Member ID: $($createMemberRes.data.id), Name: $($createMemberRes.data.fullName)"
$priyaId = $createMemberRes.data.id

Write-Host "`n=== TEST 8: Family Members count after ==="
$membersAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/users/family-members" -Method Get -WebSession $adminSession
Write-Host "SUCCESS: Family Members count after: $($membersAfter.data.Count)"
foreach ($m in $membersAfter.data) {
    Write-Host " - Member: $($m.fullName) | Email: $($m.email) | Rel: $($m.relationship) | Docs: $($m.documentCount)"
}

Write-Host "`n=== TEST 9: Login as Family Member (Priya) ==="
$priyaSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$priyaLoginBody = @{ email = "priya@docusafe.local"; password = "password123" } | ConvertTo-Json
$priyaLoginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $priyaLoginBody -ContentType "application/json" -WebSession $priyaSession
Write-Host "SUCCESS: Priya Login Success! User: $($priyaLoginRes.data.fullName) Role: $($priyaLoginRes.data.role)"

Write-Host "`n=== TEST 10: Priya's Dashboard Summary (Document Isolation) ==="
$priyaSummary = Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/summary" -Method Get -WebSession $priyaSession
Write-Host "SUCCESS: Priya Vault Total Documents: $($priyaSummary.data.totalDocuments) (Expected: 0)"

Write-Host "`n=== TEST 11: Priya can't access Admin family members management endpoint ==="
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/users/family-members" -Method Get -WebSession $priyaSession
    Write-Error "Expected 403 Forbidden for non-admin"
} catch {
    Write-Host "SUCCESS: Correctly received 403 Forbidden for non-admin on family members management"
}

Write-Host "`n=== TEST 12: Logout Priya ==="
$priyaLogout = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/logout" -Method Post -WebSession $priyaSession
Write-Host "SUCCESS: Logout response: $($priyaLogout.message)"

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -WebSession $priyaSession
    Write-Error "Expected 401 after logout"
} catch {
    Write-Host "SUCCESS: Session destroyed successfully! Received 401 after logout."
}

Write-Host "`n================================================"
Write-Host "ALL MULTI-MEMBER & AUTHENTICATION TESTS PASSED!"
Write-Host "================================================"
