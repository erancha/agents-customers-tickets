# Customer Support Hub

Spring Boot 3.x (Java 21) backend service implemented as a package-by-feature monolith (clear internal modules, single deployable).

## Prerequisites

- Java 21
- Docker (optional, for MySQL)

## Build & Run

```bash
# Start MySQL (optional; required if you use the default DB_URL)
docker compose up -d

# Build the application
./scripts/build.sh # runs Maven build (tests enabled by default). Use: SKIP_TESTS=1 ./scripts/build.sh

# Run unit/integration tests directly (without the script)
mvn test

# Run the service
java -jar target/customer-support-hub-0.0.1-SNAPSHOT.jar
```

In another terminal, you can run the end-to-end smoke test:

```bash
./scripts/smoke-test.sh
```

Default DB credentials (see `docker-compose.yml`):

- DB: `supporthub_db`
- User: `supporthub`
- Password: `supporthub123`

Service listens on `http://localhost:8080`.

## Authentication

A default admin user is created on startup:

- Username: `admin`
- Password: `admin123`

Get a JWT:

```bash
curl -s -X POST http://localhost:8080/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Use the `access_token` as `Authorization: Bearer <token>`.

## API (high level)

- `POST /api/auth/token` (public)
- `GET /api/me`, `PUT /api/me`
- `POST /api/admin/agents` (ADMIN)
- `POST /api/agent/customers` (AGENT, or ADMIN with `agentId`)
- `GET /api/agent/customers` (AGENT; ADMIN optionally filter by `agentId`)
- `POST /api/customer/tickets` (CUSTOMER)
- `GET /api/customer/tickets` (CUSTOMER)
- `GET /api/agent/tickets` (AGENT; ADMIN supports `agentId` + `customerId` filtering)

## Scripts

Scripts are available at repo root and are intended to be run from WSL.

- `./scripts/build.sh` (builds the application and runs unit/integration tests)
- `./scripts/deploy.sh`
- `./scripts/smoke-test.sh` (end-to-end smoke test; run while the service is up)
- `./scripts/undeploy.sh`
- `./scripts/clean.sh`

## Exercise requirements mapping

### Core technologies

- **Spring Boot 3.x**
  - [`pom.xml`](pom.xml)
  - [`src/main/java/com/customersupporthub/SupportHubApplication.java`](src/main/java/com/customersupporthub/SupportHubApplication.java)
- **Spring Data JPA over MySQL**: (Java Persistence API).
  - Entities / repositories:
    - [`src/main/java/com/customersupporthub/identity/infra/UserEntity.java`](src/main/java/com/customersupporthub/identity/infra/UserEntity.java)
    - [`src/main/java/com/customersupporthub/identity/infra/UserRepository.java`](src/main/java/com/customersupporthub/identity/infra/UserRepository.java)
    - [`src/main/java/com/customersupporthub/ticket/infra/TicketEntity.java`](src/main/java/com/customersupporthub/ticket/infra/TicketEntity.java)
    - [`src/main/java/com/customersupporthub/ticket/infra/TicketRepository.java`](src/main/java/com/customersupporthub/ticket/infra/TicketRepository.java)
  - MySQL configuration:
    - [`src/main/resources/application.yml`](src/main/resources/application.yml)
    - [`docker-compose.yml`](docker-compose.yml)
- **Spring Validation**
  - Request DTO annotations are used across controllers, e.g.:
    - [`src/main/java/com/customersupporthub/identity/web/AuthController.java`](src/main/java/com/customersupporthub/identity/web/AuthController.java)
    - [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java)
    - [`src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java)

### Functional scope

- **Expose a REST API via Spring MVC**
  - Controllers:
    - [`src/main/java/com/customersupporthub/identity/web/AuthController.java`](src/main/java/com/customersupporthub/identity/web/AuthController.java)
    - [`src/main/java/com/customersupporthub/identity/web/AdminUsersController.java`](src/main/java/com/customersupporthub/identity/web/AdminUsersController.java)
    - [`src/main/java/com/customersupporthub/identity/web/MeController.java`](src/main/java/com/customersupporthub/identity/web/MeController.java)
    - [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java)
    - [`src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java)
    - [`src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java)

- **Store all information in MySQL via Spring JPA**
  - Config: [`src/main/resources/application.yml`](src/main/resources/application.yml)
  - JPA entities: [`src/main/java/com/customersupporthub/identity/infra/UserEntity.java`](src/main/java/com/customersupporthub/identity/infra/UserEntity.java), [`src/main/java/com/customersupporthub/ticket/infra/TicketEntity.java`](src/main/java/com/customersupporthub/ticket/infra/TicketEntity.java)

### Role-Based Access Control (RBAC)

Roles (`ADMIN`, `AGENT`, `CUSTOMER`) are enforced through:

- **Method-Level Security**: Using `@PreAuthorize` annotations on controller methods.
- **Role Checks in Business Logic**: Explicit checks in service methods for fine-grained control.
- **JWT Claims**: Roles are embedded in JWT tokens and mapped to Spring Security authorities.

This layered security ensures that users can only access resources appropriate to their role.

- Role enum: [`src/main/java/com/customersupporthub/identity/domain/Role.java`](src/main/java/com/customersupporthub/identity/domain/Role.java)
- JWT role claim to authority mapping: [`src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java`](src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java)
- Examples of method-level security: [`src/main/java/com/customersupporthub/identity/web/AdminUsersController.java`](src/main/java/com/customersupporthub/identity/web/AdminUsersController.java), [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java), [`src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java)

### Customer management

- **Allow AGENTS only to create a new customer (under that AGENT)**
  - Endpoint + authorization + association to agent:
    - `POST /api/agent/customers`
    - [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java)
  - Persistence model for agent/customer relationship:
    - `UserEntity.agentId`
    - [`src/main/java/com/customersupporthub/identity/infra/UserEntity.java`](src/main/java/com/customersupporthub/identity/infra/UserEntity.java)

- **Allow AGENTS to query for all their customers**
  - `GET /api/agent/customers`
  - [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java)
  - [`src/main/java/com/customersupporthub/customer/application/CustomerService.java`](src/main/java/com/customersupporthub/customer/application/CustomerService.java)

- **Allow AGENTS to update their own profile**
  - `PUT /api/me`
  - [`src/main/java/com/customersupporthub/identity/web/MeController.java`](src/main/java/com/customersupporthub/identity/web/MeController.java)
  - [`src/main/java/com/customersupporthub/identity/application/IdentityService.java`](src/main/java/com/customersupporthub/identity/application/IdentityService.java)

- **Allow a CUSTOMER to query and update their own profile**
  - `GET /api/me`, `PUT /api/me`
  - [`src/main/java/com/customersupporthub/identity/web/MeController.java`](src/main/java/com/customersupporthub/identity/web/MeController.java)

### Tickets management

- **Allow customers to create new tickets and get tickets created/owned by them**
  - `POST /api/customer/tickets`, `GET /api/customer/tickets`
  - [`src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/CustomerTicketsController.java)
  - [`src/main/java/com/customersupporthub/ticket/application/TicketService.java`](src/main/java/com/customersupporthub/ticket/application/TicketService.java)

- **Allow agents to query/search tickets created by their own customers**
  - `GET /api/agent/tickets` (supports optional filtering)
  - [`src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java`](src/main/java/com/customersupporthub/ticket/web/AgentTicketsController.java)
  - [`src/main/java/com/customersupporthub/ticket/application/TicketService.java`](src/main/java/com/customersupporthub/ticket/application/TicketService.java)

### Security considerations

- **Use Spring OAuth to authenticate via username/password**
  - Token issuance endpoint:
    - `POST /api/auth/token`
    - [`src/main/java/com/customersupporthub/identity/web/AuthController.java`](src/main/java/com/customersupporthub/identity/web/AuthController.java)
  - Authentication logic:
    - [`src/main/java/com/customersupporthub/identity/application/IdentityService.java`](src/main/java/com/customersupporthub/identity/application/IdentityService.java)
  - JWT issuance + config:
    - [`src/main/java/com/customersupporthub/identity/infra/NimbusJwtService.java`](src/main/java/com/customersupporthub/identity/infra/NimbusJwtService.java)
    - [`src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java`](src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java)

### REST API behavior

- **Validate all incoming requests (Spring validation)**
  - Examples:
    - [`src/main/java/com/customersupporthub/identity/web/AuthController.java`](src/main/java/com/customersupporthub/identity/web/AuthController.java)
    - [`src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java`](src/main/java/com/customersupporthub/customer/web/AgentCustomersController.java)
    - [`src/main/java/com/customersupporthub/identity/web/MeController.java`](src/main/java/com/customersupporthub/identity/web/MeController.java)

- **Return correct HTTP statuses (200/400/401/403/404/409)**
  - Central exception-to-status mapping:
    - [`src/main/java/com/customersupporthub/shared/error/ApiExceptionHandler.java`](src/main/java/com/customersupporthub/shared/error/ApiExceptionHandler.java)
  - Custom exceptions:
    - [`src/main/java/com/customersupporthub/shared/error/ResourceNotFoundException.java`](src/main/java/com/customersupporthub/shared/error/ResourceNotFoundException.java)
    - [`src/main/java/com/customersupporthub/shared/error/ConflictException.java`](src/main/java/com/customersupporthub/shared/error/ConflictException.java)
  - Authentication/authorization (401/403):
    - [`src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java`](src/main/java/com/customersupporthub/identity/infra/SecurityConfig.java)

- **Human-readable error responses**
  - Error response model:
    - [`src/main/java/com/customersupporthub/shared/error/ApiErrorResponse.java`](src/main/java/com/customersupporthub/shared/error/ApiErrorResponse.java)
  - Error handling:
    - [`src/main/java/com/customersupporthub/shared/error/ApiExceptionHandler.java`](src/main/java/com/customersupporthub/shared/error/ApiExceptionHandler.java)

### Unit testing

- Unit tests to **some services** : [`src/test/java/com/customersupporthub/customer/application/CustomerServiceTest.java`](src/test/java/com/customersupporthub/customer/application/CustomerServiceTest.java)

- At least 1 **security-aware** unit test : `meRequiresAuthentication` in [`src/test/java/com/customersupporthub/SecurityIntegrationTest.java`](src/test/java/com/customersupporthub/SecurityIntegrationTest.java)

### Deliverables

- **[`README.md`](README.md)** (the current file) describing the project and how to build and run
- Basic **[`Dockerfile`](Dockerfile)** packaging the service
