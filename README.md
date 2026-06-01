# Kolo — Community Savings & Lending Platform

A full-stack fintech platform built for Nigeria. Groups save together weekly or monthly, the pool grows, members borrow against it — all managed by automated rules, trust scoring, and real payment infrastructure.

## Features
- Phone + OTP authentication with JWT
- Group creation, invitation, and management
- Automated contribution tracking via Paystack
- Fine engine — daily cron detects late payments
- Tier 1 & Tier 2 loan system with interest calculation
- Trust score engine based on behavioral history
- SMS notifications via Termii
- Full React web dashboard

## Tech Stack
- **Backend:** Kotlin, Spring Boot 3, PostgreSQL, Redis
- **Payments:** Paystack (virtual accounts, webhooks)
- **Infrastructure:** Docker, Railway
- **Frontend:** React, Vite, Tailwind CSS

## Architecture
Modular monolith — clean separation of concerns across Auth, Groups, Contributions, Loans, Fines, Notifications, and Trust Score modules.

## Running Locally
\```bash
docker compose up -d
./gradlew bootRun
\```

## API Documentation
Base URL: `http://localhost:8080/api`

Full endpoint list available in the codebase under each module's controller.
