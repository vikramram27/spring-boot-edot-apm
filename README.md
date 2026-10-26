# Payment Gateway REST API

A modern, high-performance Payment Gateway REST API built with **Spring Boot 3.4**, **Java 21 LTS**, **PostgreSQL 16**, **Redis 7**, and **Spring Security 6**.

Designed with fintech best practices in mind, this service delivers atomic balance transactions, guaranteed payment idempotency, instant token revocation, and a comprehensive transaction audit ledger.

---

## Key Features

- **Stateless JWT Authentication:** Secure HMAC-SHA256 token authorization with Spring Security 6.
- **Instant Token Revocation:** Redis-backed blacklist invalidates tokens immediately on logout.
- **Deposit & Top-Up Operations:** Instant balance funding supporting multiple methods (`BANK_TRANSFER`, `CREDIT_CARD`, `EWALLET`).
- **Payment Processing:** Order charge execution with real-time balance validation and overdraft prevention.
- **Guaranteed Idempotency:** Header-based `Idempotency-Key` cached in Redis (24h TTL) eliminates duplicate debits and credits from network retries.
- **Concurrency & ACID Integrity:** Optimistic locking (`@Version`) prevents race conditions and balance discrepancies under heavy traffic.
- **Audit Ledger & Refunds:** Immutable transaction history tracking all top-ups, charges, and refunds with unique reference tracking (`TOP-...`, `PAY-...`, `REF-...`).
- **Standardized Error Handling:** RFC-7807-compliant JSON error responses with detailed validation breakdowns.
- **Turnkey Containerization:** Multi-stage Docker build orchestrated via Docker Compose with automated health checks.

---

## Architecture

```
                    +-----------------------------+
                    |   Client / API Consumer     |
                    +--------------+--------------+
                                   |
                              HTTP :8088
                                   v
                    +-----------------------------+
                    |    Payment Gateway App      |
                    | (Spring Boot 3.4 / Java 21) |
                    +-------+-------------+-------+
                            |             |
                       JDBC |             | Redis RESP
                      :5432 |             | :6379
                            v             v
           +--------------------+     +--------------------+
           |   PostgreSQL 16    |     |   Redis 7 Alpine   |
           |  (ACID Relational) |     | (Idempotency Cache |
           |                    |     | & Token Blacklist) |
           +--------------------+     +--------------------+
```

---

## Services & Port Mapping

| Service | Container Port | Host Port | Purpose |
|---|---|---|---|
| `app` | `8080` | **`8088`** | Payment Gateway REST API & Health Actuator |
| `postgres` | `5432` | **`5432`** | Relational database (users, balances, ledger) |
| `redis` | `6379` | **`6380`** | Idempotency key storage & token revocation blacklist |

> **Note:** Host ports `8088` and `6380` prevent port collisions with other local containers.

---

## Getting Started

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) & [Docker Compose v2](https://docs.docker.com/compose/)
- `curl` (for manual testing and verification)

### 1. Start the Cluster
```bash
docker compose up -d --build
```

### 2. Verify Service Health
```bash
curl -s http://localhost:8088/actuator/health | jq .
```
Expected output:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### 3. Run Automated End-to-End Verification
```bash
./verify-api.sh
```
This script executes a 13-stage test suite validating the entire lifecycle: registration, authentication, deposits, idempotency deduplication, payments, insufficient balance rejection, refunds, transaction queries, and token logout revocation.

---

## Configuration & Environment Variables

Environment variables can be configured directly or via `.env`:

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Internal application port |
| `DB_HOST` | `postgres` | PostgreSQL hostname |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `edot_apm` | Database schema name |
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `redis` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | *(256-bit hex)* | HMAC-SHA256 signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token validity duration (24 hours) |

---

## API Reference

All requests and responses use `Content-Type: application/json`.  
Protected endpoints require the standard header: `Authorization: Bearer <JWT_TOKEN>`.

### Common Response Wrapper
Successful responses are encapsulated in a standard envelope:
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": "2026-10-26T10:00:00Z"
}
```

---

### 1. Authentication (`/api/v1/auth`)

#### Register a New Account
Creates a new customer profile and initializes an active payment balance.
- **Method:** `POST`
- **Path:** `/api/v1/auth/register`
- **Auth:** Public

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex Morgan",
    "email": "alex.morgan@example.com",
    "password": "SecurePassword123!"
  }'
```

**Response (`201 Created`):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "18b1a1c7-4736-441b-b709-274c9c5a91c6",
      "name": "Alex Morgan",
      "email": "alex.morgan@example.com",
      "role": "ROLE_USER",
      "createdAt": "2026-10-26T10:00:00Z"
    }
  }
}
```

---

#### User Login
Authenticates credentials and returns a JWT access token.
- **Method:** `POST`
- **Path:** `/api/v1/auth/login`
- **Auth:** Public

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alex.morgan@example.com",
    "password": "SecurePassword123!"
  }'
```

---

#### User Logout
Immediately revokes the JWT token by adding it to the Redis blacklist for its remaining lifespan.
- **Method:** `POST`
- **Path:** `/api/v1/auth/logout`
- **Auth:** Bearer Token

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/auth/logout \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 2. User Management (`/api/v1/users`)

#### Get Current Profile
- **Method:** `GET`
- **Path:** `/api/v1/users/me`
- **Auth:** Bearer Token

```bash
curl -X GET http://localhost:8088/api/v1/users/me \
  -H "Authorization: Bearer <TOKEN>"
```

#### Update Profile
- **Method:** `PUT`
- **Path:** `/api/v1/users/me`
- **Auth:** Bearer Token

```bash
curl -X PUT http://localhost:8088/api/v1/users/me \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Alex Morgan Updated"}'
```

---

### 3. Balance & Payment Operations (`/api/v1/pocket`)

#### Get Account Balance
Retrieves current available funds and currency.
- **Method:** `GET`
- **Path:** `/api/v1/pocket`
- **Auth:** Bearer Token

**Request:**
```bash
curl -X GET http://localhost:8088/api/v1/pocket \
  -H "Authorization: Bearer <TOKEN>"
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Pocket retrieved successfully",
  "data": {
    "pocketId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "userId": "18b1a1c7-4736-441b-b709-274c9c5a91c6",
    "balance": 150.00,
    "currency": "USD",
    "updatedAt": "2026-10-26T10:10:00Z"
  }
}
```

---

#### Top-Up / Deposit Funds
Credit funds to the account. Supports optional `Idempotency-Key` header to safely retry requests without risk of duplicate deposits.
- **Method:** `POST`
- **Path:** `/api/v1/pocket/top-up`
- **Auth:** Bearer Token
- **Headers:** `Idempotency-Key: <unique-string>` (Recommended)

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/pocket/top-up \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: topup-req-20261026-001" \
  -d '{
    "amount": 250.00,
    "paymentMethod": "BANK_TRANSFER",
    "description": "Wire transfer deposit"
  }'
```

**Response (`201 Created`):**
```json
{
  "success": true,
  "message": "Pocket top-up completed successfully",
  "data": {
    "id": "dff17ea8-30a3-4cab-8e26-bc60ab77041f",
    "referenceId": "TOP-F0FC8816C6A8",
    "type": "TOP_UP",
    "amount": 250.00,
    "balanceAfter": 250.00,
    "currency": "USD",
    "status": "SUCCESS",
    "paymentMethod": "BANK_TRANSFER",
    "orderId": null,
    "idempotencyKey": "topup-req-20261026-001",
    "description": "Wire transfer deposit",
    "createdAt": "2026-10-26T10:10:33Z"
  }
}
```

---

#### Process Payment Charge
Debit funds from the account to pay for an order. Fails atomically if balance is insufficient.
- **Method:** `POST`
- **Path:** `/api/v1/pocket/pay`
- **Auth:** Bearer Token
- **Headers:** `Idempotency-Key: <unique-string>` (Recommended)

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/pocket/pay \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-pay-20261026-99" \
  -d '{
    "amount": 45.50,
    "orderId": "ORD-2026-9999",
    "description": "Order payment for Merchant XYZ"
  }'
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "id": "6e6fe4ae-782f-403b-931c-0cb4091225b7",
    "referenceId": "PAY-EBB9326071EA",
    "type": "PAYMENT",
    "amount": 45.50,
    "balanceAfter": 204.50,
    "currency": "USD",
    "status": "SUCCESS",
    "paymentMethod": "POCKET",
    "orderId": "ORD-2026-9999",
    "idempotencyKey": "order-pay-20261026-99",
    "description": "Order payment for Merchant XYZ",
    "createdAt": "2026-10-26T10:10:33Z"
  }
}
```

---

#### List Transaction Ledger
Retrieves full chronological ledger of all customer deposits, payments, and refunds.
- **Method:** `GET`
- **Path:** `/api/v1/pocket/transactions`
- **Auth:** Bearer Token

**Request:**
```bash
curl -X GET http://localhost:8088/api/v1/pocket/transactions \
  -H "Authorization: Bearer <TOKEN>"
```

---

#### Refund a Transaction
Refunds a previously completed payment and credits the exact amount back to the customer's balance.
- **Method:** `POST`
- **Path:** `/api/v1/pocket/transactions/{transactionId}/refund`
- **Auth:** Bearer Token

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/pocket/transactions/6e6fe4ae-782f-403b-931c-0cb4091225b7/refund \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer cancelled order before shipping"
  }'
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Transaction refunded successfully",
  "data": {
    "id": "7729dc85-f869-4c20-a723-8e839de37162",
    "referenceId": "REF-PAY-EBB9326071EA",
    "type": "REFUND",
    "amount": 45.50,
    "balanceAfter": 250.00,
    "currency": "USD",
    "status": "SUCCESS",
    "paymentMethod": "POCKET",
    "orderId": "ORD-2026-9999",
    "description": "Refund for PAY-EBB9326071EA (Customer cancelled order before shipping)",
    "createdAt": "2026-10-26T10:10:33Z"
  }
}
```

---

## Error Handling

Errors follow consistent JSON structures with descriptive error codes and HTTP status values:

### Insufficient Balance (`400 Bad Request`)
```json
{
  "success": false,
  "status": 400,
  "error": "INSUFFICIENT_BALANCE",
  "message": "Insufficient pocket balance. Required: USD 5000.00, Available: USD 104.50",
  "validationErrors": null,
  "timestamp": "2026-10-26T10:10:33Z"
}
```

### Validation Error (`400 Bad Request`)
```json
{
  "success": false,
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed for request fields",
  "validationErrors": {
    "email": "Email must be valid",
    "password": "Password must be at least 6 characters"
  },
  "timestamp": "2026-10-26T10:10:33Z"
}
```

### Unauthorized / Token Revoked (`401 Unauthorized`)
```json
{
  "success": false,
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentication is required to access this resource",
  "validationErrors": null,
  "timestamp": "2026-10-26T10:10:33Z"
}
```

---

## Code Quality & Best Practices

This project adheres to the [`java-springboot` best practices](.agents/skills/java-springboot/SKILL.md):
- **Domain Modularization:** Code structured by functional domain rather than monolithic layers.
- **Dependency Inversion:** Explicit constructor injection with immutable `private final` fields.
- **DTO Isolation:** Database JPA entities are never leaked to API consumers.
- **Transactional Consistency:** Granular `@Transactional` boundaries on all state mutations.
- **Optimized Logging:** High-throughput parameterized SLF4J logging across all layers.
