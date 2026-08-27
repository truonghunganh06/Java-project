# GRAFT CONTEXT & CODE EXPLORATION RULES

## 1. Context Exploration Principles
- **Targeted Symbol Lookup First**: Avoid loading whole large source files when inspecting method contracts. Use symbol queries, repo maps, or signatures.
- **Blast Radius & Dependency Trace**: Before editing or deleting public methods, repositories, or entity columns, trace all references across services, controllers, templates, and integration tests.
- **Cache & Freshening**: Treat generated architecture maps as local caches; always verify against actual code bytes before finalizing modifications.

## 2. Spring Boot Multi-tier Architecture Guidelines
- **Entity Layer**: Maintain unique constraints and relationships with clear fetch strategies.
- **Repository Layer**: Leverage Spring Data JPA interface methods and indexed queries.
- **Service Layer**: Wrap business actions with `@Transactional`, validate domain preconditions, and handle conflict-of-interest checks.
- **Controller & UI**: Provide clear feedback to users via flash attributes and validated forms.
