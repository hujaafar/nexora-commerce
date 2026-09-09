> Retained educational reference from the component projects. For current commands, architecture, and verification, use the root README and VALIDATION.md.

<!--
File purpose: Indexes every tracked file, what it contains, and what to learn from it.
-->
# Nexora Commerce file-by-file learning guide

Use this guide as a map before opening an unfamiliar file. Hand-written files
that safely support comments contain their purpose at the top; the related study
concept stays in this guide. Strict JSON, generated wrappers/lockfiles, and the
binary favicon are explained here instead of being modified, because inline
comments would either break their parser or create noisy generated diffs.

## How to study a file

1. Read its **File purpose** to locate it in the request or build flow.
2. Read its **What to learn** and identify that pattern in the implementation.
3. Use `git log --follow -- <file>` to see the concept evolve commit by commit.
4. Run the nearest test after experimenting.

## Complete tracked-file index

| File | File purpose | What to learn | Documentation |
|---|---|---|---|
| `.dockerignore` | Defines repository/tooling rules through .dockerignore. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `.editorconfig` | Defines repository/tooling rules through .editorconfig. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `.env.example` | Lists safe example environment variables for local deployment. | Externalized secrets, port overrides, origins, and rate-limit tuning. | Existing inline header |
| `.gitattributes` | Defines repository/tooling rules through .gitattributes. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `.gitignore` | Defines repository/tooling rules through .gitignore. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `.mvn/wrapper/maven-wrapper.properties` | Generated tooling or binary asset required by the project. | Generated artifacts should be used and versioned when needed, but not hand-edited. | Guide only (generated/binary) |
| `README.md` | Explains how to run, use, secure, test, and study the complete marketplace. | Operational documentation and maintainable project handoff. | Existing inline header |
| `compose.https.yml` | Adds the HTTPS ingress override to the base Compose stack. | Container networks, health-based dependencies, volumes, environment configuration, and overrides. | Existing inline header |
| `compose.yml` | Orchestrates every application and infrastructure dependency locally. | Container networks, health-based dependencies, volumes, environment configuration, and overrides. | Existing inline header |
| `deploy/Caddyfile` | Terminates HTTPS and forwards public traffic to the frontend container. | Automatic TLS certificate provisioning and secure reverse proxying. | Existing inline header |
| `discovery-service/pom.xml` | Configures discovery-service dependencies and plugins. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `discovery-service/src/main/java/com/nexora/discovery/DiscoveryServiceApplication.java` | Bootstraps the discovery-service Spring application. | Spring Boot auto-configuration and executable service entry points. | Existing inline header |
| `discovery-service/src/main/resources/application.yml` | Externalizes runtime settings for discovery-service. | Spring profiles, environment-variable overrides, health checks, and service discovery. | Existing inline header |
| `discovery-service/src/test/java/com/nexora/discovery/DiscoveryServiceApplicationTests.java` | Verifies discovery service application tests behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `docker/Dockerfile.backend` | Builds any selected Spring module into a minimal runtime image. | Multi-stage container builds, dependency caching, and non-root runtimes. | Existing inline header |
| `docs/LEARNING_PATH.md` | Documents the project learning path. | Living documentation and traceability alongside implementation. | Existing inline header |
| `docs/REQUIREMENTS_CHECKLIST.md` | Documents the project requirements checklist. | Living documentation and traceability alongside implementation. | Existing inline header |
| `docs/UI_DESIGN_GUIDE.md` | Documents the project ui design guide. | Living documentation and traceability alongside implementation. | Existing inline header |
| `docs/api-examples.http` | Provides executable HTTP examples for every public and protected API. | API exploration, bearer authentication, multipart requests, and moderation calls. | Existing inline header |
| `docs/architecture.md` | Documents the project architecture. | Living documentation and traceability alongside implementation. | Existing inline header |
| `docs/security.md` | Documents the project security. | Living documentation and traceability alongside implementation. | Existing inline header |
| `docs/troubleshooting.md` | Documents the project troubleshooting. | Living documentation and traceability alongside implementation. | Existing inline header |
| `frontend/.dockerignore` | Defines repository/tooling rules through .dockerignore. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `frontend/.editorconfig` | Defines repository/tooling rules through .editorconfig. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `frontend/.gitignore` | Defines repository/tooling rules through .gitignore. | Repository hygiene, consistent text handling, and small build contexts. | Existing inline header |
| `frontend/.prettierrc` | Defines automated frontend formatting preferences. | Consistent formatting as a team-level constraint. | Guide only (strict JSON) |
| `frontend/Dockerfile` | Builds Angular and serves the optimized files with Nginx. | Multi-stage container builds, dependency caching, and non-root runtimes. | Existing inline header |
| `frontend/README.md` | Records frontend-specific Angular commands and structure. | Operational documentation and maintainable project handoff. | Existing inline header |
| `frontend/angular.json` | Configures Angular build, test, asset, and bundle-budget targets. | Angular workspace configuration and production budgets. | Guide only (strict JSON) |
| `frontend/nginx.conf` | Serves the SPA and proxies browser API calls to the gateway. | Reverse proxies, SPA fallback routing, cache headers, security headers, and upload limits. | Existing inline header |
| `frontend/package-lock.json` | Generated tooling or binary asset required by the project. | Generated artifacts should be used and versioned when needed, but not hand-edited. | Guide only (generated/binary) |
| `frontend/package.json` | Declares frontend scripts and npm dependencies. | Semantic dependency management and repeatable npm scripts. | Guide only (strict JSON) |
| `frontend/proxy.conf.json` | Maps Angular development API calls to the local gateway. | Development reverse proxies and same-origin API calls. | Guide only (strict JSON) |
| `frontend/public/favicon.ico` | Generated tooling or binary asset required by the project. | Generated artifacts should be used and versioned when needed, but not hand-edited. | Guide only (generated/binary) |
| `frontend/src/app/app.config.ts` | Registers application-wide Angular providers. | Standalone Angular bootstrap configuration and functional providers. | Existing inline header |
| `frontend/src/app/app.html` | Defines global navigation, routed content, notifications, and footer. | Semantic application shells and Angular template control flow. | Existing inline header |
| `frontend/src/app/app.routes.ts` | Defines lazy application routes and their guards. | Route-level code splitting and authorization. | Existing inline header |
| `frontend/src/app/app.scss` | Styles the global authenticated application shell. | Responsive navigation, shared visual states, and layout composition. | Existing inline header |
| `frontend/src/app/app.spec.ts` | Verifies app.spec behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `frontend/src/app/app.ts` | Controls the global application shell and session actions. | Root components, signals, router events, and shared navigation. | Existing inline header |
| `frontend/src/app/core/guards/admin.guard.ts` | Protects Angular routes that require admin access. | Functional route guards and role-aware navigation. | Existing inline header |
| `frontend/src/app/core/guards/auth.guard.ts` | Protects Angular routes that require auth access. | Functional route guards and role-aware navigation. | Existing inline header |
| `frontend/src/app/core/guards/seller.guard.ts` | Protects Angular routes that require seller access. | Functional route guards and role-aware navigation. | Existing inline header |
| `frontend/src/app/core/interceptors/auth.interceptor.ts` | Attaches the stored bearer token to API requests. | Functional HTTP interceptors for cross-cutting client behavior. | Existing inline header |
| `frontend/src/app/core/interceptors/error.interceptor.ts` | Handles common HTTP authorization and API failures. | Functional HTTP interceptors for cross-cutting client behavior. | Existing inline header |
| `frontend/src/app/core/services/admin.service.ts` | Centralizes admin API or UI state operations. | Typed HttpClient services, observables, and separation from components. | Existing inline header |
| `frontend/src/app/core/services/auth.service.ts` | Centralizes auth API or UI state operations. | Typed HttpClient services, observables, and separation from components. | Existing inline header |
| `frontend/src/app/core/services/media.service.spec.ts` | Verifies media.service.spec behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `frontend/src/app/core/services/media.service.ts` | Centralizes media API or UI state operations. | Typed HttpClient services, observables, and separation from components. | Existing inline header |
| `frontend/src/app/core/services/notification.service.ts` | Centralizes notification API or UI state operations. | Signal-based shared UI state and transient notifications. | Existing inline header |
| `frontend/src/app/core/services/product.service.ts` | Centralizes product API or UI state operations. | Typed HttpClient services, observables, and separation from components. | Existing inline header |
| `frontend/src/app/features/admin/admin-dashboard.html` | Defines the accessible admin feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/admin/admin-dashboard.scss` | Styles the responsive admin feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/admin/admin-dashboard.ts` | Implements the admin feature behavior. | Standalone Angular components, parallel API loading, signals, and moderation actions. | Existing inline header |
| `frontend/src/app/features/auth/auth-shared.scss` | Styles the responsive auth feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/auth/login/login.html` | Defines the accessible auth feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/auth/login/login.scss` | Styles the responsive auth feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/auth/login/login.ts` | Implements the auth feature behavior. | Reactive Forms, validation, role selection, and navigation after authentication. | Existing inline header |
| `frontend/src/app/features/auth/register/register.html` | Defines the accessible auth feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/auth/register/register.scss` | Styles the responsive auth feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/auth/register/register.ts` | Implements the auth feature behavior. | Reactive Forms, validation, role selection, and navigation after authentication. | Existing inline header |
| `frontend/src/app/features/media/media-manager/media-manager.html` | Defines the accessible media feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/media/media-manager/media-manager.scss` | Styles the responsive media feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/media/media-manager/media-manager.ts` | Implements the media feature behavior. | File previews, client-side validation, upload progress states, and computed signals. | Existing inline header |
| `frontend/src/app/features/products/product-detail/product-detail.html` | Defines the accessible products feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/products/product-detail/product-detail.scss` | Styles the responsive products feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/products/product-detail/product-detail.spec.ts` | Verifies product detail.spec behavior. | Testing asynchronous loading and error UI states. | Existing inline header |
| `frontend/src/app/features/products/product-detail/product-detail.ts` | Implements the products feature behavior. | Signals, API loading states, route parameters, and custom requestAnimationFrame scrolling. | Existing inline header |
| `frontend/src/app/features/products/product-list/product-list.html` | Defines the accessible products feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/products/product-list/product-list.scss` | Styles the responsive products feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/products/product-list/product-list.spec.ts` | Verifies product list.spec behavior. | Testing multi-frame UI motion and navigation destinations. | Existing inline header |
| `frontend/src/app/features/products/product-list/product-list.ts` | Implements the products feature behavior. | Signals, API loading states, route parameters, and custom requestAnimationFrame scrolling. | Existing inline header |
| `frontend/src/app/features/profile/profile.html` | Defines the accessible profile feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/profile/profile.scss` | Styles the responsive profile feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/profile/profile.ts` | Implements the profile feature behavior. | Profile state, image previews, avatar replacement, and multipart uploads. | Existing inline header |
| `frontend/src/app/features/seller/dashboard/seller-dashboard.html` | Defines the accessible seller feature template. | Angular template control flow, semantic HTML, bindings, and accessible interactions. | Existing inline header |
| `frontend/src/app/features/seller/dashboard/seller-dashboard.scss` | Styles the responsive seller feature experience. | Component-scoped Sass, responsive layouts, states, and motion design. | Existing inline header |
| `frontend/src/app/features/seller/dashboard/seller-dashboard.ts` | Implements the seller feature behavior. | Reactive Forms, computed inventory metrics, and multi-step product/image workflows. | Existing inline header |
| `frontend/src/app/models/api-error.model.ts` | Defines TypeScript contracts for api error data. | End-to-end type safety between Angular and backend DTOs. | Existing inline header |
| `frontend/src/app/models/media.model.ts` | Defines TypeScript contracts for media data. | End-to-end type safety between Angular and backend DTOs. | Existing inline header |
| `frontend/src/app/models/product.model.ts` | Defines TypeScript contracts for product data. | End-to-end type safety between Angular and backend DTOs. | Existing inline header |
| `frontend/src/app/models/user.model.ts` | Defines TypeScript contracts for user data. | End-to-end type safety between Angular and backend DTOs. | Existing inline header |
| `frontend/src/environments/environment.ts` | Defines the frontend API base path for the production build. | Environment-specific client configuration. | Existing inline header |
| `frontend/src/index.html` | Provides the browser document that hosts the Angular root component. | SPA document shells, metadata, and resource loading. | Existing inline header |
| `frontend/src/main.ts` | Bootstraps the standalone Angular application in the browser. | Angular application startup without NgModules. | Existing inline header |
| `frontend/src/styles.scss` | Defines global tokens, typography, controls, utilities, and motion preferences. | Design systems, CSS custom properties, and accessible global styling. | Existing inline header |
| `frontend/tsconfig.app.json` | Narrows TypeScript compilation to production application sources. | Specialized compiler targets for an Angular application. | Guide only (strict JSON) |
| `frontend/tsconfig.json` | Defines shared TypeScript compiler behavior. | Strict typing and compiler configuration inheritance. | Guide only (strict JSON) |
| `frontend/tsconfig.spec.json` | Narrows TypeScript compilation to test sources. | Separate compiler environments for application and tests. | Guide only (strict JSON) |
| `gateway-service/pom.xml` | Configures gateway-service dependencies and plugins. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/GatewayServiceApplication.java` | Bootstraps the gateway-service Spring application. | Spring Boot auto-configuration and executable service entry points. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/config/CorsConfig.java` | Creates and configures cors config. | Externalized configuration and dependency creation with Spring beans. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/config/GatewayRoutesConfig.java` | Creates and configures gateway routes config. | Externalized configuration and dependency creation with Spring beans. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/config/GatewaySecurityConfig.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/filter/RateLimitWebFilter.java` | Limits authentication and media-write request bursts per client. | Reactive WebFilters, token-bucket rate limiting, and 429 responses. | Existing inline header |
| `gateway-service/src/main/java/com/nexora/gateway/filter/RequestIdFilter.java` | Adds or propagates a correlation ID for each request. | Cross-cutting observability at the API edge. | Existing inline header |
| `gateway-service/src/main/resources/application.yml` | Externalizes runtime settings for gateway-service. | Spring profiles, environment-variable overrides, health checks, and service discovery. | Existing inline header |
| `gateway-service/src/test/java/com/nexora/gateway/GatewayServiceApplicationTests.java` | Verifies gateway service application tests behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `gateway-service/src/test/java/com/nexora/gateway/filter/RateLimitWebFilterTest.java` | Verifies rate limit web filter test behavior. | Testing time-based edge protection without a live server. | Existing inline header |
| `media-service/pom.xml` | Configures media-service dependencies and plugins. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/MediaServiceApplication.java` | Bootstraps the media-service Spring application. | Spring Boot auto-configuration and executable service entry points. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/config/ApiSecurityConfig.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/config/ObjectStorageConfig.java` | Creates and configures object storage config. | Externalized configuration and dependency creation with Spring beans. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/config/ObjectStorageInitializer.java` | Creates and configures object storage initializer. | Externalized configuration and dependency creation with Spring beans. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/domain/MediaAsset.java` | Models the media asset domain concept persisted or used by the service. | Domain modeling, MongoDB documents, indexes, and explicit enums. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/domain/MediaPurpose.java` | Models the media purpose domain concept persisted or used by the service. | Domain modeling, MongoDB documents, indexes, and explicit enums. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/dto/MediaDownload.java` | Defines the media download API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/dto/MediaResponse.java` | Defines the media response API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/event/MediaEvent.java` | Defines the immutable Kafka event envelope. | Stable event contracts for asynchronous consumers. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/event/MediaEventPublisher.java` | Publishes service lifecycle events to Kafka. | Asynchronous messaging, topic configuration, and loose service coupling. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/exception/InvalidMediaException.java` | Represents the invalid media exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/exception/MediaNotFoundException.java` | Represents the media not found exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/exception/ObjectStorageException.java` | Represents the object storage exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/repository/MediaAssetRepository.java` | Provides persistence queries for media asset data. | Spring Data repository abstraction and query derivation. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/service/MediaService.java` | Implements media service business rules. | Service-layer orchestration, JWT-subject ownership, persistence, and events. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/storage/ObjectStorage.java` | Defines the object-storage port used by the media domain. | Ports-and-adapters design and keeping object bytes outside MongoDB. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/storage/S3ObjectStorage.java` | Implements image-byte storage using the S3-compatible SDK. | Ports-and-adapters design and keeping object bytes outside MongoDB. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/validation/DetectedImageType.java` | Validates or normalizes uploaded image data through detected image type. | Secure file handling with allowlists, magic bytes, and path-safe names. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/validation/FilenameSanitizer.java` | Validates or normalizes uploaded image data through filename sanitizer. | Secure file handling with allowlists, magic bytes, and path-safe names. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/validation/ImageSignatureValidator.java` | Validates or normalizes uploaded image data through image signature validator. | Secure file handling with allowlists, magic bytes, and path-safe names. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/web/ApiError.java` | Defines the consistent error response returned by the service. | Predictable API error contracts for frontend consumers. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/web/ApiExceptionHandler.java` | Translates validation and domain failures into stable JSON errors. | Centralized @RestControllerAdvice and safe error boundaries. | Existing inline header |
| `media-service/src/main/java/com/nexora/media/web/MediaController.java` | Exposes media HTTP endpoints. | Thin REST controllers, request validation, status codes, and delegated business logic. | Existing inline header |
| `media-service/src/main/resources/application.yml` | Externalizes runtime settings for media-service. | Spring profiles, environment-variable overrides, health checks, and service discovery. | Existing inline header |
| `media-service/src/test/java/com/nexora/media/service/MediaServiceTest.java` | Verifies media service test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `media-service/src/test/java/com/nexora/media/validation/ImageSignatureValidatorTest.java` | Verifies image signature validator test behavior. | Adversarial file-signature and MIME validation tests. | Existing inline header |
| `media-service/src/test/java/com/nexora/media/web/ApiExceptionHandlerTest.java` | Verifies api exception handler test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `mvnw` | Generated tooling or binary asset required by the project. | Generated artifacts should be used and versioned when needed, but not hand-edited. | Guide only (generated/binary) |
| `mvnw.cmd` | Generated tooling or binary asset required by the project. | Generated artifacts should be used and versioned when needed, but not hand-edited. | Guide only (generated/binary) |
| `pom.xml` | Configures the multi-module Maven build. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `product-service/pom.xml` | Configures product-service dependencies and plugins. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/ProductServiceApplication.java` | Bootstraps the product-service Spring application. | Spring Boot auto-configuration and executable service entry points. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/config/ApiSecurityConfig.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/domain/Product.java` | Models the product domain concept persisted or used by the service. | Domain modeling, MongoDB documents, indexes, and explicit enums. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/dto/ProductRequest.java` | Defines the product request API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/dto/ProductResponse.java` | Defines the product response API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/event/ProductEvent.java` | Defines the immutable Kafka event envelope. | Stable event contracts for asynchronous consumers. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/event/ProductEventPublisher.java` | Publishes service lifecycle events to Kafka. | Asynchronous messaging, topic configuration, and loose service coupling. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/exception/ProductNotFoundException.java` | Represents the product not found exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/repository/ProductRepository.java` | Provides persistence queries for product data. | Spring Data repository abstraction and query derivation. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/service/ProductService.java` | Implements product service business rules. | Service-layer orchestration, JWT-subject ownership, persistence, and events. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/web/ApiError.java` | Defines the consistent error response returned by the service. | Predictable API error contracts for frontend consumers. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/web/ApiExceptionHandler.java` | Translates validation and domain failures into stable JSON errors. | Centralized @RestControllerAdvice and safe error boundaries. | Existing inline header |
| `product-service/src/main/java/com/nexora/product/web/ProductController.java` | Exposes product HTTP endpoints. | Thin REST controllers, request validation, status codes, and delegated business logic. | Existing inline header |
| `product-service/src/main/resources/application.yml` | Externalizes runtime settings for product-service. | Spring profiles, environment-variable overrides, health checks, and service discovery. | Existing inline header |
| `product-service/src/test/java/com/nexora/product/service/ProductServiceTest.java` | Verifies product service test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `product-service/src/test/java/com/nexora/product/web/ApiExceptionHandlerTest.java` | Verifies api exception handler test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `scripts/clear-product-posts.ps1` | Removes product posts and their linked media through authorized APIs. | Safe scoped cleanup with confirmation, authentication, and recoverable targeting. | Existing inline header |
| `scripts/smoke-test.ps1` | Exercises the complete deployed marketplace and cleans temporary data. | End-to-end production-path testing across every service. | Existing inline header |
| `scripts/start.ps1` | Builds and starts the complete Compose platform from PowerShell. | Developer workflow automation and health-aware startup. | Existing inline header |
| `scripts/start.sh` | Builds and starts the complete Compose platform from a POSIX shell. | Cross-platform developer workflow automation. | Existing inline header |
| `scripts/stop.ps1` | Stops the Compose platform while preserving development volumes. | Safe lifecycle automation and persistent data. | Existing inline header |
| `scripts/stop.sh` | Stops the Compose platform from a POSIX shell. | Cross-platform lifecycle automation. | Existing inline header |
| `scripts/test.ps1` | Runs reproducible backend and frontend verification. | Containerized build toolchains and full regression suites. | Existing inline header |
| `user-service/pom.xml` | Configures user-service dependencies and plugins. | Maven dependency management, plugins, and reproducible Java builds. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/UserServiceApplication.java` | Bootstraps the user-service Spring application. | Spring Boot auto-configuration and executable service entry points. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/config/DemoDataSeeder.java` | Creates and configures demo data seeder. | Externalized configuration and dependency creation with Spring beans. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/domain/Role.java` | Models the role domain concept persisted or used by the service. | Domain modeling, MongoDB documents, indexes, and explicit enums. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/domain/UserAccount.java` | Models the user account domain concept persisted or used by the service. | Domain modeling, MongoDB documents, indexes, and explicit enums. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/AuthResponse.java` | Defines the auth response API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/LoginRequest.java` | Defines the login request API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/RegisterRequest.java` | Defines the register request API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/RegistrationRole.java` | Defines the registration role API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/UpdateProfileRequest.java` | Defines the update profile request API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/dto/UserResponse.java` | Defines the user response API data contract. | Immutable record DTOs, boundary validation, and avoiding domain-object exposure. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/exception/DuplicateEmailException.java` | Represents the duplicate email exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/exception/UserNotFoundException.java` | Represents the user not found exception domain failure. | Typed domain exceptions that map cleanly to meaningful HTTP responses. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/repository/UserAccountRepository.java` | Provides persistence queries for user account data. | Spring Data repository abstraction and query derivation. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/security/ApiSecurityConfig.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/security/JwtConfig.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/security/JwtService.java` | Defines authentication, authorization, JWT, or HTTP security rules. | Defense in depth with Spring Security, resource-server JWT validation, and role rules. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/service/AuthService.java` | Implements auth service business rules. | Service-layer orchestration, password security, identity, and profile rules. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/service/ProfileService.java` | Implements profile service business rules. | Service-layer orchestration, password security, identity, and profile rules. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/web/AdminController.java` | Exposes admin HTTP endpoints. | Thin REST controllers, request validation, status codes, and delegated business logic. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/web/ApiError.java` | Defines the consistent error response returned by the service. | Predictable API error contracts for frontend consumers. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/web/ApiExceptionHandler.java` | Translates validation and domain failures into stable JSON errors. | Centralized @RestControllerAdvice and safe error boundaries. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/web/AuthController.java` | Exposes auth HTTP endpoints. | Thin REST controllers, request validation, status codes, and delegated business logic. | Existing inline header |
| `user-service/src/main/java/com/nexora/user/web/ProfileController.java` | Exposes profile HTTP endpoints. | Thin REST controllers, request validation, status codes, and delegated business logic. | Existing inline header |
| `user-service/src/main/resources/application.yml` | Externalizes runtime settings for user-service. | Spring profiles, environment-variable overrides, health checks, and service discovery. | Existing inline header |
| `user-service/src/test/java/com/nexora/user/service/AuthServiceTest.java` | Verifies auth service test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `user-service/src/test/java/com/nexora/user/service/ProfileServiceTest.java` | Verifies profile service test behavior. | Isolated regression testing and behavior-focused assertions. | Existing inline header |
| `docs/FILE_GUIDE.md` | Indexes every tracked file and its learning value. | Maintaining navigable architecture documentation as the codebase grows. | This guide |
