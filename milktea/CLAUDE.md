# CLAUDE.md — Instructions for Claude in MilkTea Multi-Branch System

This is the central entry point for Claude working on the MilkTea Chain project.

## Single Source of Truth
Do NOT redefine business rules or pricing logic here. Refer to:
- [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md) for domain rules, roles, pricing, inventory, and reviews.
- [docs/REQUIREMENTS_TRACEABILITY.md](docs/REQUIREMENTS_TRACEABILITY.md) for requirement codes and acceptance tests.
- [docs/BRANCH_DEVELOPMENT_PLAN.md](docs/BRANCH_DEVELOPMENT_PLAN.md) for phased implementation plan.
- [docs/IMPLEMENTATION_PROGRESS.md](docs/IMPLEMENTATION_PROGRESS.md) for progress and checkpoints.
- [docs/MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md) for safe SQL Server migration guidelines.

## Key Development Rules
1. **Stack**: Spring Boot MVC monolith (Java 21, Thymeleaf, Bootstrap, JPA, SQL Server, STOMP WebSocket, Cloudinary). No microservices, no SPA.
2. **Database**: Never run DROP DATABASE or destructive table resets on the live `milktea` database. Use incremental, audited SQL migration scripts.
3. **Backend Authorization**: Verify both user Role and Branch Scope at the service layer. Never trust client-supplied branchId, prices, or ownership.
4. **Idempotency & Transactions**: Protect critical operations (order creation, payment, refund, stock ledger) using `RequestDedupRecord` (`BanGhiChongLap`). Record failed OTP attempts without rollback.
5. **Realtime**: Branch-scoped topics `/topic/branches/{branchId}/cashier` and `/topic/branches/{branchId}/kitchen` with authorization on SUBSCRIBE.
