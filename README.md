# 💳 Secure Payment System

A transaction-safe payment processing REST API built with Spring Boot, demonstrating enterprise-grade patterns used in real fintech systems.

---

## 🏗️ Architecture Overview

```
Client → JWT Auth Filter → Controller → Service → Repository → MySQL
                                           ↓
                                  Fraud Detection Engine
                                  Audit Logger
```

---

## ✨ Key Features

| Feature | Implementation |
|---|---|
| **ACID Transactions** | `@Transactional` on all payment operations — entire flow commits or rolls back atomically |
| **Idempotent APIs** | `idempotencyKey` on every transaction — duplicate requests return same result, never double-charge |
| **Pessimistic Locking** | `SELECT FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)` on accounts — prevents concurrent balance corruption |
| **Optimistic Locking** | `@Version` on Account entity — extra safety layer for low-conflict reads |
| **Fraud Detection** | Real-time velocity checks (count & amount per hour) + amount threshold rules |
| **Audit Logging** | Every transaction logged with user, timestamp, before/after balance |
| **JWT Authentication** | Stateless auth — no sessions, scales horizontally |
| **Docker Ready** | Multi-stage Dockerfile + docker-compose with MySQL |

---

## 🚀 Running the Project

### Option 1 — Docker (Recommended, zero setup)

```bash
docker-compose up --build
```

App starts at `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Option 2 — Local

1. Install MySQL 8 and create database:
```sql
CREATE DATABASE payment_system;
```

2. Update `src/main/resources/application.yml`:
```yaml
spring.datasource.username: your_mysql_user
spring.datasource.password: your_mysql_password
```

3. Run:
```bash
mvn spring-boot:run
```

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Accounts
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Create new account |
| GET | `/api/accounts` | Get all my accounts |
| GET | `/api/accounts/{accountNumber}` | Get account details |

### Payments
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/process` | Process a payment |

---

## 🔁 Sample Flow

**1. Register**
```json
POST /api/auth/register
{
  "email": "alice@example.com",
  "password": "Alice@1234",
  "firstName": "Alice",
  "lastName": "Smith"
}
```

**2. Login → get token**
```json
POST /api/auth/login
{ "email": "alice@example.com", "password": "Alice@1234" }
→ { "token": "eyJ..." }
```

**3. Create account** *(use Bearer token in header)*
```json
POST /api/accounts
Authorization: Bearer eyJ...
{ "accountType": "SAVINGS", "initialDeposit": 10000.00 }
→ { "accountNumber": "PAY123456789" }
```

**4. Send payment** *(idempotencyKey prevents double-send)*
```json
POST /api/payments/process
Authorization: Bearer eyJ...
{
  "fromAccountNumber": "PAY123456789",
  "toAccountNumber": "PAY987654321",
  "amount": 500.00,
  "idempotencyKey": "unique-uuid-per-payment-attempt",
  "description": "Rent payment"
}
```

---

## 🛡️ Fraud Detection Rules

| Rule | Threshold | Risk | Action |
|---|---|---|---|
| Single transaction amount | > ₹1,00,000 | HIGH | Blocked |
| Transaction count/hour | > 10 | MEDIUM | Alert raised |
| Total amount/hour | > ₹5,00,000 | HIGH | Blocked |

---

## 🧱 Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **MySQL 8** with **Hibernate/JPA**
- **Spring Security** + **JWT (JJWT 0.12)**
- **Docker** + **Docker Compose**
- **Swagger/OpenAPI** (auto-generated docs)
- **Lombok**, **Bean Validation**

---

## 📁 Project Structure

```
src/main/java/com/payment/
├── controller/        # REST endpoints (AuthController, PaymentController, AccountController)
├── service/           # Business logic (AuthService, PaymentService, AccountService)
├── entity/            # JPA entities (User, Account, Transaction, AuditLog, FraudAlert)
├── repository/        # Spring Data JPA repositories
├── dto/               # Request/Response objects
├── security/          # JWT filter, SecurityConfig, @CurrentUser
└── exception/         # GlobalExceptionHandler, ResourceNotFoundException
```
