# AGENTS.md — Agent System Guide for MilkTea Multi-Branch System

This is the unified entry point for all AI agents working on the MilkTea Chain project.

## 1. Single Source of Truth
Never maintain diverging copies of business logic. Always reference:
- Rules & Policies: [docs/BUSINESS_RULES.md](docs/BUSINESS_RULES.md)
- Traceability & Verification: [docs/REQUIREMENTS_TRACEABILITY.md](docs/REQUIREMENTS_TRACEABILITY.md)
- Development Roadmap: [docs/BRANCH_DEVELOPMENT_PLAN.md](docs/BRANCH_DEVELOPMENT_PLAN.md)
- Progress & Checkpoints: [docs/IMPLEMENTATION_PROGRESS.md](docs/IMPLEMENTATION_PROGRESS.md)
- Safe Data Migration: [docs/MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md)
- Codex Guidelines: [CODEX.md](CODEX.md)

## 2. Agent Principles
- Keep the monolithic Spring Boot MVC + single SQL Server database architecture.
- Do NOT perform destructive resets on the live `milktea` database.
- Backend must strictly enforce branch-scoped authorization and idempotency.
- Maintain documentation integrity and update progress checkpoints after each development phase.
