# Kolo — Community Savings & Lending Platform

> A production-grade fintech backend built for Nigeria's cooperative savings culture (*ajo/esusu*).  
> Groups save together, the pool grows, members borrow against it — managed by automated rules, trust scoring, and real payment infrastructure.

[![CI](https://github.com/Nelsonobazee12/Kolo-backend-MVP/actions/workflows/ci.yml/badge.svg)](https://github.com/Nelsonobazee12/Kolo-backend-MVP/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=spring)
![Paystack](https://img.shields.io/badge/Payments-Paystack-00C3F7)

**Live API:** `https://kolo-backend.up.railway.app`  
**Swagger UI:** `https://kolo-backend.up.railway.app/swagger-ui/index.html`

---

## What is Kolo?

Kolo is a backend system for rotating savings groups common in Nigeria. In a typical *ajo* group, 10 members each contribute ₦10,000 monthly — each month, one member receives the full ₦100,000 pool. Kolo automates the financial mechanics that make this trustworthy at scale:

- Automated contribution tracking and receipt via Paystack virtual accounts
- A **Trust Score engine** that numerically rates member reliability based on payment history, fines, loan repayment, and tenure — the behavioral graph that informal groups track mentally
- A **fine engine** that runs nightly via cron, detects late payments, and levies penalties automatically
- A tiered loan system where borrowing limits are gated by trust score

This isn't a tutorial CRUD app. It models real financial behavior with real payment infrastructure.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     REST API Layer                        │
│         (Spring MVC + Spring Security + JWT)              │
└───────────────────────┬──────────────────────────────────┘
                        │
        ┌───────────────┴────────────────┐
        │         Service Layer          │
        │                                │
   ┌────┴────┐  ┌────────┐  ┌─────────┐ │
   │  Auth   │  │ Groups │  │ Loans   │ │
   │ Module  │  │ Module │  │ Module  │ │
   └────┬────┘  └────┬───┘  └────┬────┘ │
        │            │           │      │
   ┌────┴────┐  ┌────┴───┐  ┌────┴────┐ │
   │ Trust   │  │ Fines  │  │Notifs   │ │
   │ Score   │  │ Engine │  │(Termii) │ │
   └─────────┘  └────────┘  └─────────┘ │
        └───────────────┬────────────────┘
                        │
   ┌────────────────────┴────────────────────┐
   │              Data Layer                  │
   │   PostgreSQL (JPA)  │  Redis (sessions)  │
   └────────────────────┬────────────────────┘
                        │
   ┌────────────────────┴────────────────────┐
   │            External Services             │
   │  Paystack (payments) │ Termii (SMS/OTP)  │
   └──────────────────────────────────────────┘
```

**Design pattern:** Modular monolith — each domain (Auth, Groups, Contributions, Loans, Fines, Trust, Notifications) is a self-contained package with its own controller, service, and repository. Easy to reason about, easy to extract into microservices later.

---

## Key Features

### Trust Score Engine
Every member has a dynamically computed trust score (0–100) derived from:
- On-time contribution rate
- Fine history (count and recency)
- Loan repayment history
- Group tenure

The score gates loan eligibility. Tier 1 loans (up to 50% of contribution pool) require a score ≥ 60. Tier 2 loans (up to 100%) require ≥ 80. This models the informal reputation system that real *ajo* groups run on instinct — Kolo makes it auditable.

### Fine Engine
A scheduled cron job runs nightly at midnight. It queries all active groups for unpaid contributions past their due date and automatically creates fine records. Members are notified via SMS. This prevents the "chasing people manually" problem that kills informal savings groups.

### Paystack Virtual Accounts
Each member is assigned a dedicated Paystack virtual account number. Contributions are made by bank transfer — Paystack's webhook fires on receipt, the system matches the amount to the member and group, and marks the contribution as paid. No manual reconciliation.

### Loan System
- Tier 1: up to 50% of pool, trust score ≥ 60, repayable within group cycle
- Tier 2: up to 100% of pool, trust score ≥ 80, approved by group admin
- Interest calculated at time of disbursement, repayment tracked per installment

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Kotlin + Spring Boot 3.x | Concise, null-safe, modern JVM |
| Database | PostgreSQL + Spring Data JPA | Relational integrity for financial data |
| Migrations | Flyway | Versioned schema — no `ddl-auto=create` |
| Auth | Spring Security + JWT (jjwt) | Stateless, refresh token rotation |
| Sessions | Redis | Fast token invalidation on logout |
| Payments | Paystack | Nigerian bank infrastructure, virtual accounts |
| SMS / OTP | Termii | Nigerian telecoms coverage |
| Docs | springdoc-openapi (Swagger UI) | Self-documenting API |
| Infra | Docker + Railway | Reproducible deploys |

**Why Paystack over Stripe?**  
Stripe has limited support for NGN settlements and Nigerian bank accounts. Paystack is purpose-built for Nigeria — it supports virtual account creation (NUBAN), USSD payments, and local bank transfers. This was a deliberate infrastructure choice, not a workaround.

---

## Running Locally

**Prerequisites:** Docker, Java 21

```bash
# 1. Clone and set up environment
git clone https://github.com/Nelsonobazee12/Kolo-backend-MVP.git
cd Kolo-backend-MVP
cp .env.example .env
# Fill in your Paystack test keys and Termii keys

# 2. Start Postgres + Redis
docker compose up -d

# 3. Run the app
./gradlew bootRun
```

API is live at `http://localhost:8080`  
Swagger UI at `http://localhost:8080/swagger-ui/index.html`

---

## API Overview

| Module | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| Groups | `POST /api/groups`, `GET /api/groups/{id}`, `/join`, `/invite` |
| Contributions | `GET /api/contributions`, `POST /api/contributions/verify` |
| Loans | `POST /api/loans/apply`, `GET /api/loans/{id}`, `/repay` |
| Fines | `GET /api/fines`, `POST /api/fines/{id}/pay` |
| Trust Score | `GET /api/users/{id}/trust-score` |
| Admin | Full group and member management |

Full request/response schemas available in Swagger UI.

---

## Design Decisions

**Why a modular monolith over microservices?**  
This is an MVP. Microservices add operational overhead (service discovery, distributed tracing, network latency) that isn't justified at this stage. The modular structure means each domain could be extracted if traffic warranted it — but for now, a single deployable unit is simpler to reason about and debug.

**Why Flyway for migrations?**  
`spring.jpa.hibernate.ddl-auto=update` is fine for a tutorial but dangerous in production — it can silently drop columns or miss constraints. Flyway gives a versioned, auditable schema history. Every change is a numbered migration file.

**Why Redis for session management?**  
Stateless JWT is good but doesn't support instant token revocation (e.g. on logout or account suspension). Redis stores active refresh tokens with TTL, so a compromised token can be invalidated immediately without waiting for it to expire.

---

## Project Status

This is an MVP. Known limitations and planned improvements:

- [ ] Integration test suite (in progress)
- [ ] Group cycle automation (payout rotation logic)
- [ ] Admin dashboard frontend
- [ ] Rate limiting on auth endpoints
- [ ] Audit log for all financial transactions

---

## Author

**Nelson Obazee** — Backend Engineer  
[GitHub](https://github.com/Nelsonobazee12) · [X / Twitter](https://x.com/ThisIsPREX)