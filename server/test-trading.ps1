# ============================================
# Monkey Business Trading Platform Test Script
# PowerShell version - Fixed syntax
# ============================================

$ErrorActionPreference = "Stop"

Write-Host "`n=== MONKEY BUSINESS TRADING PLATFORM ===" -ForegroundColor Cyan

# 1. CREATE USER
Write-Host "`nStep 1: Creating user..." -ForegroundColor Green
$userBody = @{
    username = "trader1"
    name = "John Trader"
    email = "trader1@test.com"
    password = "Test@1234"
} | ConvertTo-Json

$userResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/users" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $userBody | Select-Object -ExpandProperty Content | ConvertFrom-Json

$USER_ID = $userResponse.userId
Write-Host "✓ User created: $USER_ID"

# 2. CREATE ACCOUNT
Write-Host "`nStep 2: Creating account with $50,000..." -ForegroundColor Green
$accountBody = @{
    accountType = "BROKERAGE"
    initialBalance = 50000
    initialCashBalance = 50000
} | ConvertTo-Json

$accountResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/users/$USER_ID/accounts" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $accountBody | Select-Object -ExpandProperty Content | ConvertFrom-Json

$ACCOUNT_ID = $accountResponse.accountId
Write-Host "✓ Account created: $ACCOUNT_ID"
Write-Host "  Initial Balance: `$$($accountResponse.balance)"
Write-Host "  Initial Cash: `$$($accountResponse.cashBalance)"

# 3. CHECK MARKET PRICE
Write-Host "`nStep 3: Checking AAPL market price..." -ForegroundColor Green
$priceResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/market/price/AAPL" `
  | Select-Object -ExpandProperty Content | ConvertFrom-Json

$AAPL_PRICE = $priceResponse.price
Write-Host "✓ AAPL Price: `$$AAPL_PRICE"

# 4. BUY 10 SHARES
Write-Host "`nStep 4: Buying 10 shares of AAPL..." -ForegroundColor Green
$buyBody = @{
    orderType = "EQUITY"
    action = "BUY"
    ticker = "AAPL"
    quantity = 10
} | ConvertTo-Json

$buyResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID/orders" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $buyBody | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ BUY Order placed"
Write-Host "  Order ID: $($buyResponse.orderId)"
Write-Host "  Status: $($buyResponse.status)"
Write-Host "  Submitted Value: `$$($buyResponse.submittedValue)"

# 5. CHECK ACCOUNT AFTER BUY
Write-Host "`nStep 5: Checking account balance after BUY..." -ForegroundColor Green
$accountCheck1 = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID" `
  | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ Account updated"
Write-Host "  Total Balance: `$$($accountCheck1.balance)"
Write-Host "  Cash Balance: `$$($accountCheck1.cashBalance)"

# 6. LIST ORDERS
Write-Host "`nStep 6: Listing all orders..." -ForegroundColor Green
$ordersResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID/orders" `
  | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ Total Orders: $($ordersResponse.Count)"
if ($ordersResponse -is [array]) {
    $ordersResponse | ForEach-Object {
        Write-Host "  - $($_.action) $($_.quantity) $($_.ticker) @ Status: $($_.status)"
    }
} else {
    Write-Host "  - $($ordersResponse.action) $($ordersResponse.quantity) $($ordersResponse.ticker) @ Status: $($ordersResponse.status)"
}

# 7. SELL 5 SHARES
Write-Host "`nStep 7: Selling 5 shares of AAPL..." -ForegroundColor Green
$sellBody = @{
    orderType = "EQUITY"
    action = "SELL"
    ticker = "AAPL"
    quantity = 5
} | ConvertTo-Json

$sellResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID/orders" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $sellBody | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ SELL Order placed"
Write-Host "  Order ID: $($sellResponse.orderId)"
Write-Host "  Status: $($sellResponse.status)"
Write-Host "  Submitted Value: `$$($sellResponse.submittedValue)"

# 8. CHECK FINAL ACCOUNT STATE
Write-Host "`nStep 8: Final account state..." -ForegroundColor Green
$accountFinal = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID" `
  | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ Final State"
Write-Host "  Total Balance: `$$($accountFinal.balance)"
Write-Host "  Cash Balance: `$$($accountFinal.cashBalance)"

# 9. LIST ALL ORDERS AGAIN
Write-Host "`nStep 9: Final order list..." -ForegroundColor Green
$finalOrders = Invoke-WebRequest -Uri "http://localhost:8080/api/accounts/$ACCOUNT_ID/orders" `
  | Select-Object -ExpandProperty Content | ConvertFrom-Json

Write-Host "✓ Total Orders: $($finalOrders.Count)"
if ($finalOrders -is [array]) {
    $finalOrders | ForEach-Object {
        Write-Host "  - $($_.action) $($_.quantity) $($_.ticker) @ $($_.status)"
    }
} else {
    Write-Host "  - $($finalOrders.action) $($finalOrders.quantity) $($finalOrders.ticker) @ $($finalOrders.status)"
}

Write-Host "`n=== TEST COMPLETE ===" -ForegroundColor Cyan
Write-Host ""
