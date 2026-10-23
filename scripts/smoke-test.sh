#!/usr/bin/env sh
set -eu
ORDER=$(curl -fsS -X POST http://localhost:8080/api/orders -H 'Content-Type: application/json' -d '{"customerId":"portfolio-user","sku":"SKU-001","quantity":2}')
ID=$(printf '%s' "$ORDER" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
sleep 3
curl -fsS "http://localhost:8080/api/shipments/$ID" | grep READY_TO_SHIP
echo 'Smoke test passed.'

