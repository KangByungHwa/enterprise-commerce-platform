$ErrorActionPreference = 'Stop'
$body = @{ customerId='portfolio-user'; sku='SKU-001'; quantity=2 } | ConvertTo-Json
$order = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders' -ContentType 'application/json' -Body $body
Write-Host "Order created:" $order.id
Start-Sleep -Seconds 3
$shipment = Invoke-RestMethod -Uri ("http://localhost:8080/api/shipments/" + $order.id)
if ($shipment.state -ne 'READY_TO_SHIP') { throw 'Shipment projection was not created' }
Write-Host 'Smoke test passed.'

