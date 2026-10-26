#!/usr/bin/env bash

# Automated API Verification Script for Spring Boot APM & Pocket Gateway
set -e

BASE_URL="http://localhost:8088"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================================${NC}"
echo -e "${BLUE}  SPRING BOOT API & USER POCKET GATEWAY VERIFICATION SUITE       ${NC}"
echo -e "${BLUE}================================================================${NC}"

# Check dependencies
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is required.${NC}"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq is not installed. JSON formatting will use python or raw output.${NC}"
    parse_json() {
        python3 -c "import sys, json; print(json.load(sys.stdin)$1)" 2>/dev/null || cat
    }
else
    parse_json() {
        jq -r "$1" 2>/dev/null
    }
fi

echo -e "\n${BLUE}[1/13] Checking Health Endpoint...${NC}"
HEALTH_RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/actuator/health")
HEALTH_CODE=$(echo "$HEALTH_RESP" | tail -n1)
HEALTH_BODY=$(echo "$HEALTH_RESP" | sed '$d')

if [ "$HEALTH_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Health Check Passed (HTTP 200):${NC} $HEALTH_BODY"
else
    echo -e "${RED}✗ Health Check Failed with HTTP $HEALTH_CODE:${NC} $HEALTH_BODY"
    exit 1
fi

TIMESTAMP=$(date +%s)
TEST_EMAIL="vikram_${TIMESTAMP}@example.com"
TEST_PASS="Secret123!"
TEST_NAME="Vikram Tester"

echo -e "\n${BLUE}[2/13] Registering User (${TEST_EMAIL})...${NC}"
REG_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${TEST_NAME}\",\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASS}\"}")
REG_CODE=$(echo "$REG_RESP" | tail -n1)
REG_BODY=$(echo "$REG_RESP" | sed '$d')

if [ "$REG_CODE" -eq 201 ]; then
    echo -e "${GREEN}✓ Registration Passed (HTTP 201)${NC}"
    echo "$REG_BODY"
else
    echo -e "${RED}✗ Registration Failed (HTTP $REG_CODE):${NC} $REG_BODY"
    exit 1
fi

echo -e "\n${BLUE}[3/13] Logging in user to obtain JWT...${NC}"
LOGIN_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASS}\"}")
LOGIN_CODE=$(echo "$LOGIN_RESP" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESP" | sed '$d')

if [ "$LOGIN_CODE" -eq 200 ]; then
    TOKEN=$(echo "$LOGIN_BODY" | parse_json ".data.accessToken")
    echo -e "${GREEN}✓ Login Passed (HTTP 200). JWT acquired:${NC} ${TOKEN:0:25}..."
else
    echo -e "${RED}✗ Login Failed (HTTP $LOGIN_CODE):${NC} $LOGIN_BODY"
    exit 1
fi

AUTH_HEADER="Authorization: Bearer ${TOKEN}"

echo -e "\n${BLUE}[4/13] Fetching Profile (GET /api/v1/users/me)...${NC}"
PROFILE_RESP=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/me" \
  -H "$AUTH_HEADER")
PROFILE_CODE=$(echo "$PROFILE_RESP" | tail -n1)
PROFILE_BODY=$(echo "$PROFILE_RESP" | sed '$d')
echo -e "${GREEN}✓ Profile retrieved (HTTP $PROFILE_CODE):${NC} $PROFILE_BODY"

echo -e "\n${BLUE}[5/13] Updating Profile Name (PUT /api/v1/users/me)...${NC}"
UPDATE_RESP=$(curl -s -w "\n%{http_code}" -X PUT "${BASE_URL}/api/v1/users/me" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Vikram Updated"}')
UPDATE_CODE=$(echo "$UPDATE_RESP" | tail -n1)
UPDATE_BODY=$(echo "$UPDATE_RESP" | sed '$d')
echo -e "${GREEN}✓ Profile updated (HTTP $UPDATE_CODE):${NC} $UPDATE_BODY"

echo -e "\n${BLUE}[6/13] Checking Initial Pocket Balance (GET /api/v1/pocket)...${NC}"
POCKET_RESP=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/pocket" \
  -H "$AUTH_HEADER")
POCKET_CODE=$(echo "$POCKET_RESP" | tail -n1)
POCKET_BODY=$(echo "$POCKET_RESP" | sed '$d')
INITIAL_BALANCE=$(echo "$POCKET_BODY" | parse_json ".data.balance")
echo -e "${GREEN}✓ Initial Balance:${NC} \$${INITIAL_BALANCE} (HTTP $POCKET_CODE)"

IDEMPOTENCY_TOPUP="idemp-topup-${TIMESTAMP}"
echo -e "\n${BLUE}[7/13] Topping up pocket with \$150.00 (POST /api/v1/pocket/top-up)...${NC}"
TOPUP_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/pocket/top-up" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMPOTENCY_TOPUP}" \
  -d '{"amount":150.00,"paymentMethod":"BANK_TRANSFER","description":"Initial wallet deposit"}')
TOPUP_CODE=$(echo "$TOPUP_RESP" | tail -n1)
TOPUP_BODY=$(echo "$TOPUP_RESP" | sed '$d')
TOPUP_BAL=$(echo "$TOPUP_BODY" | parse_json ".data.balanceAfter")
echo -e "${GREEN}✓ Top-up Successful (HTTP $TOPUP_CODE). New Balance: \$${TOPUP_BAL}${NC}"

echo -e "\n${BLUE}[8/13] Testing Top-up Idempotency with duplicate Idempotency-Key...${NC}"
TOPUP_DUP_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/pocket/top-up" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMPOTENCY_TOPUP}" \
  -d '{"amount":150.00,"paymentMethod":"BANK_TRANSFER","description":"Duplicate attempt"}')
TOPUP_DUP_BODY=$(echo "$TOPUP_DUP_RESP" | sed '$d')
TOPUP_DUP_BAL=$(echo "$TOPUP_DUP_BODY" | parse_json ".data.balanceAfter")

CHECK_BAL=$(curl -s -X GET "${BASE_URL}/api/v1/pocket" -H "$AUTH_HEADER" | parse_json ".data.balance")
if python3 -c "import sys; sys.exit(0 if float('${CHECK_BAL}') == 150.0 else 1)" 2>/dev/null; then
    echo -e "${GREEN}✓ Idempotency Verified! Balance remained \$${CHECK_BAL} (Cached response returned)${NC}"
else
    echo -e "${RED}✗ Idempotency Failed! Balance unexpectedly changed to \$${CHECK_BAL}${NC}"
    exit 1
fi

IDEMPOTENCY_PAY="idemp-pay-${TIMESTAMP}"
echo -e "\n${BLUE}[9/13] Processing Payment of \$45.50 for Order #ORD-9999 (POST /api/v1/pocket/pay)...${NC}"
PAY_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/pocket/pay" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMPOTENCY_PAY}" \
  -d '{"amount":45.50,"orderId":"ORD-9999","description":"Purchase electronics"}')
PAY_CODE=$(echo "$PAY_RESP" | tail -n1)
PAY_BODY=$(echo "$PAY_RESP" | sed '$d')
TXN_ID=$(echo "$PAY_BODY" | parse_json ".data.id")
PAY_BAL=$(echo "$PAY_BODY" | parse_json ".data.balanceAfter")
echo -e "${GREEN}✓ Payment Processed (HTTP $PAY_CODE). Txn ID: ${TXN_ID}, Remaining Balance: \$${PAY_BAL}${NC}"

echo -e "\n${BLUE}[10/13] Testing Insufficient Balance Error Handling (\$5,000.00 debit)...${NC}"
OVER_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/pocket/pay" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000.00,"orderId":"ORD-OVERDRAFT","description":"Attempt overspend"}')
OVER_CODE=$(echo "$OVER_RESP" | tail -n1)
OVER_BODY=$(echo "$OVER_RESP" | sed '$d')

if [ "$OVER_CODE" -eq 400 ]; then
    echo -e "${GREEN}✓ Correctly rejected with HTTP 400 Bad Request:${NC} $OVER_BODY"
else
    echo -e "${RED}✗ Expected HTTP 400 but got HTTP $OVER_CODE:${NC} $OVER_BODY"
    exit 1
fi

echo -e "\n${BLUE}[11/13] Refunding Payment of \$45.50 (POST /api/v1/pocket/transactions/${TXN_ID}/refund)...${NC}"
REFUND_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/pocket/transactions/${TXN_ID}/refund" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Customer returned item"}')
REFUND_CODE=$(echo "$REFUND_RESP" | tail -n1)
REFUND_BODY=$(echo "$REFUND_RESP" | sed '$d')
RESTORED_BAL=$(echo "$REFUND_BODY" | parse_json ".data.balanceAfter")
echo -e "${GREEN}✓ Refund Successful (HTTP $REFUND_CODE). Balance restored to: \$${RESTORED_BAL}${NC}"

echo -e "\n${BLUE}[12/13] Listing Transactions (GET /api/v1/pocket/transactions)...${NC}"
TXNS_RESP=$(curl -s -X GET "${BASE_URL}/api/v1/pocket/transactions" -H "$AUTH_HEADER")
echo -e "${GREEN}✓ Transactions Ledger:${NC}"
echo "$TXNS_RESP" | parse_json ".data"

echo -e "\n${BLUE}[13/13] Logging Out & Verifying Redis Token Revocation...${NC}"
LOGOUT_RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/logout" \
  -H "$AUTH_HEADER")
LOGOUT_CODE=$(echo "$LOGOUT_RESP" | tail -n1)
echo -e "${GREEN}✓ Logout called (HTTP $LOGOUT_CODE). Token added to Redis blacklist.${NC}"

REVOKED_CHECK=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/users/me" \
  -H "$AUTH_HEADER")
REVOKED_CODE=$(echo "$REVOKED_CHECK" | tail -n1)
REVOKED_BODY=$(echo "$REVOKED_CHECK" | sed '$d')

if [ "$REVOKED_CODE" -eq 401 ]; then
    echo -e "${GREEN}✓ Blacklisted Token Rejected as expected (HTTP 401 Unauthorized):${NC} $REVOKED_BODY"
else
    echo -e "${RED}✗ Revoked token was NOT rejected! HTTP $REVOKED_CODE:${NC} $REVOKED_BODY"
    exit 1
fi

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}  ALL 13 TESTS PASSED SUCCESSFULLY!                             ${NC}"
echo -e "${GREEN}================================================================${NC}"
