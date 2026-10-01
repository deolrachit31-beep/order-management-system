#!/usr/bin/env bash
set -euo pipefail

USER_URL="${USER_URL:-http://localhost:8081}"
PRODUCT_URL="${PRODUCT_URL:-http://localhost:8082}"
ORDER_URL="${ORDER_URL:-http://localhost:8083}"
PAYMENT_URL="${PAYMENT_URL:-http://localhost:8084}"
NOTIFICATION_URL="${NOTIFICATION_URL:-http://localhost:8085}"

wait_for() {
  local url="$1"
  local attempt
  for attempt in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "Ready: $url"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for $url" >&2
  exit 1
}

json_field() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

wait_for "$USER_URL/actuator/health"
wait_for "$PRODUCT_URL/actuator/health"
wait_for "$ORDER_URL/actuator/health"
wait_for "$PAYMENT_URL/actuator/health"
wait_for "$NOTIFICATION_URL/actuator/health"

USER_ID="$(curl -fsS -X POST "$USER_URL/api/users" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ava Shah","email":"ava.shah@example.com","phone":"+1-555-0100"}' | json_field id)"
echo "User: $USER_ID"

PRODUCT_ID="$(curl -fsS -X POST "$PRODUCT_URL/api/products" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Notebook","description":"Hardcover lab notebook","price":12.50,"stockQuantity":25}' | json_field id)"
echo "Product: $PRODUCT_ID"

ORDER_ID="$(curl -fsS -X POST "$ORDER_URL/api/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$USER_ID\",\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}]}" | json_field id)"
echo "Order: $ORDER_ID"

STATUS=""
for attempt in $(seq 1 30); do
  STATUS="$(curl -fsS "$ORDER_URL/api/orders/$ORDER_ID" | json_field status)"
  echo "Order status: $STATUS"
  if [ "$STATUS" = "PAID" ] || [ "$STATUS" = "PAYMENT_FAILED" ]; then
    break
  fi
  sleep 1
done

echo "Payment:"
curl -fsS "$PAYMENT_URL/api/payments?orderId=$ORDER_ID"
echo
echo "Notifications:"
curl -fsS "$NOTIFICATION_URL/api/notifications?orderId=$ORDER_ID"
echo

if [ "$STATUS" != "PAID" ]; then
  echo "Expected order status PAID but found $STATUS" >&2
  exit 1
fi
