# CODEX.md — Codex Instructions for MilkTea Multi-Branch System

This file provides code generation instructions for OpenAI Codex in the MilkTea project.

## Reference Documentation
- Domain Rules: [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md)
- Traceability & Test Codes: [docs/REQUIREMENTS_TRACEABILITY.md](docs/REQUIREMENTS_TRACEABILITY.md)
- Phased Plan: [docs/BRANCH_DEVELOPMENT_PLAN.md](docs/BRANCH_DEVELOPMENT_PLAN.md)
- Safe Migration: [docs/MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md)

## Coding Conventions
1. **Java & Framework**: Java 21, Spring Boot 4.x/3.x, Spring Data JPA, Spring Security 6.x.
2. **Database Queries**: Microsoft SQL Server dialect. Use `SYSUTCDATETIME()`, `BIGINT`, `DECIMAL(18,2)` or `DECIMAL(18,3)` for monetary and quantity values.
3. **Transactions**: Use `@Transactional`. For background audit or OTP failure recording that must survive caller rollback, use `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
4. **Idempotency**: Lookup and save records in `BanGhiChongLap` (`RequestDedupRecord`). Compare SHA-256 hash of payload.
5. **No Hallucinated Classes**: Use actual repository classes: `OrderRepository`, `DiningTableRepository`, `TableSessionRepository`, `StockRepository`, `PaymentRepository`.
