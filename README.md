# BUY-01 Marketplace

An end-to-end learning project built as independently deployable Spring Boot
microservices with an Angular single-page application.

The repository is intentionally developed in small, concept-focused Git commits.
Follow `docs/LEARNING_PATH.md` and run `git log --oneline --reverse` to learn why
each part exists.

## Applications

| Application | Port | Responsibility |
|---|---:|---|
| API Gateway | 8080 | External API, JWT checks, CORS, routing |
| Discovery Service | 8761 | Eureka service registry |
| User Service | 8081 | Registration, login, profiles, roles |
| Product Service | 8082 | Public catalog and seller-owned CRUD |
| Media Service | 8083 | Validated image upload and object storage |
| Angular UI | 4200 | Public catalog and seller workspace |

Full setup, API examples, architecture, security decisions, and troubleshooting
will be documented as each learning slice is added.
