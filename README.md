# Agents Customers Tickets

Spring Boot 3.x (Java 21) backend service implemented as a package-by-feature monolith (clear internal modules, single deployable).

## Prerequisites

- Java 21
- Docker (optional, for MySQL)

## Build & Run

```bash
# Start only MySQL (recommended when running Spring Boot from IDE/CLI)
docker compose up -d mysql

# Optional: start full stack (MySQL + app service defined in compose)
docker compose up -d

# Build the application
./scripts/build.sh # runs Maven build (tests enabled by default). Use: SKIP_TESTS=1 ./scripts/build.sh

# Run unit/integration tests directly (without the script)
mvn test

# Run the service
java -jar target/agents-customers-tickets-0.0.1-SNAPSHOT.jar
```

In another terminal, you can run the end-to-end smoke test:

```bash
./scripts/smoke-test.sh
```

Default DB credentials (see `docker-compose.yml`):

- DB: `agentscustomerstickets_db`
- User: `agentscustomerstickets`
- Password: `agentscustomerstickets123`

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
- `POST /api/agents` (ADMIN)
- `POST /api/customers` (AGENT, or ADMIN with `agentId`)
- `GET /api/customers` (AGENT; ADMIN optionally filter by `agentId`)
- `POST /api/tickets` (CUSTOMER)
- `GET /api/tickets` (supports role-based filtering: CUSTOMER sees own, AGENT sees assigned, ADMIN can filter by `agentId` + `customerId`)

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
  - [`src/main/java/com/agentscustomerstickets/Application.java`](src/main/java/com/agentscustomerstickets/Application.java)
- **Spring Data JPA over MySQL**: (Java Persistence API).
  - Entities / repositories:
    - [`src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java)
    - [`src/main/java/com/agentscustomerstickets/identity/infra/UserRepository.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserRepository.java)
    - [`src/main/java/com/agentscustomerstickets/ticket/infra/TicketEntity.java`](src/main/java/com/agentscustomerstickets/tickets/infra/TicketEntity.java)
    - [`src/main/java/com/agentscustomerstickets/ticket/infra/TicketRepository.java`](src/main/java/com/agentscustomerstickets/tickets/infra/TicketRepository.java)
  - MySQL configuration:
    - [`src/main/resources/application.yml`](src/main/resources/application.yml)
    - [`docker-compose.yml`](docker-compose.yml)
- **Spring Validation**
  - Request DTO annotations are used across controllers, e.g.:
    - [`src/main/java/com/agentscustomerstickets/identity/web/AuthController.java`](src/main/java/com/agentscustomerstickets/identity/web/AuthController.java)
    - [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java)
    - [`src/main/java/com/agentscustomerstickets/ticket/web/TicketsController.java`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)

### Functional scope

- **Expose a REST API via Spring MVC**
  - Controllers:
    - [`src/main/java/com/agentscustomerstickets/identity/web/AuthController.java`](src/main/java/com/agentscustomerstickets/identity/web/AuthController.java)
    - [`src/main/java/com/agentscustomerstickets/identity/web/MeController.java`](src/main/java/com/agentscustomerstickets/identity/web/MeController.java)
    - [`src/main/java/com/agentscustomerstickets/agent/web/AgentController.java`](src/main/java/com/agentscustomerstickets/agents/web/AgentController.java)
    - [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java)
    - [`src/main/java/com/agentscustomerstickets/ticket/web/TicketsController.java`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)

- **Store all information in MySQL via Spring JPA**
  - Config: [`src/main/resources/application.yml`](src/main/resources/application.yml)
  - JPA entities: [`src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java), [`src/main/java/com/agentscustomerstickets/ticket/infra/TicketEntity.java`](src/main/java/com/agentscustomerstickets/tickets/infra/TicketEntity.java)

### Role-Based Access Control (RBAC)

Roles (`ADMIN`, `AGENT`, `CUSTOMER`) are enforced through:

- **Method-Level Security**: Using `@PreAuthorize` annotations on controller methods.
- **Role Checks in Business Logic**: Explicit checks in service methods for fine-grained control.
- **JWT Claims**: Roles are embedded in JWT tokens and mapped to Spring Security authorities.

This layered security ensures that users can only access resources appropriate to their role.

- Role enum: [`src/main/java/com/agentscustomerstickets/identity/domain/Role.java`](src/main/java/com/agentscustomerstickets/identity/domain/Role.java)
- JWT role claim to authority mapping: [`src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java`](src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java)
- Examples of method-level security: [`src/main/java/com/agentscustomerstickets/agent/web/AgentController.java`](src/main/java/com/agentscustomerstickets/agents/web/AgentController.java), [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java), [`src/main/java/com/agentscustomerstickets/ticket/web/TicketsController.java`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)

### Customer management

- **Allow AGENTS only to create a new customer (under that AGENT)**
  - Endpoint + authorization + association to agent:
    - `POST /api/customers`
    - [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java)
  - Persistence model for agent/customer relationship:
    - `UserEntity.agentId`
    - [`src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java)

- **Allow AGENTS to query for all their customers**
  - `GET /api/customers`
  - [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java)
  - [`src/main/java/com/agentscustomerstickets/customer/application/CustomerService.java`](src/main/java/com/agentscustomerstickets/customers/application/CustomerService.java)

- **Allow AGENTS to update their own profile**
  - `PUT /api/me`
  - [`src/main/java/com/agentscustomerstickets/identity/web/MeController.java`](src/main/java/com/agentscustomerstickets/identity/web/MeController.java)
  - [`src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java`](src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java)

- **Allow a CUSTOMER to query and update their own profile**
  - `GET /api/me`, `PUT /api/me`
  - [`src/main/java/com/agentscustomerstickets/identity/web/MeController.java`](src/main/java/com/agentscustomerstickets/identity/web/MeController.java)

### Tickets management

- **Allow customers to create new tickets and get tickets created/owned by them**
  - `POST /api/tickets` (create), `GET /api/tickets` (list customer's tickets)
  - [`src/main/java/com/agentscustomerstickets/ticket/web/TicketsController.java`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)
  - [`src/main/java/com/agentscustomerstickets/ticket/application/TicketService.java`](src/main/java/com/agentscustomerstickets/tickets/application/TicketService.java)

- **Allow agents to query/search tickets created by their own customers**
  - `GET /api/tickets` (agents see assigned tickets; supports optional `customerId` filtering)
  - [`src/main/java/com/agentscustomerstickets/ticket/web/TicketsController.java`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)
  - [`src/main/java/com/agentscustomerstickets/ticket/application/TicketService.java`](src/main/java/com/agentscustomerstickets/tickets/application/TicketService.java)

### Security considerations

- **Use Spring OAuth to authenticate via username/password**
  - Token issuance endpoint:
    - `POST /api/auth/token`
    - [`src/main/java/com/agentscustomerstickets/identity/web/AuthController.java`](src/main/java/com/agentscustomerstickets/identity/web/AuthController.java)
  - Authentication logic:
    - [`src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java`](src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java)
  - JWT issuance + config:
    - [`src/main/java/com/agentscustomerstickets/identity/infra/NimbusJwtService.java`](src/main/java/com/agentscustomerstickets/identity/infra/NimbusJwtService.java)
    - [`src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java`](src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java)

### REST API behavior

- **Validate all incoming requests (Spring validation)**
  - Examples:
    - [`src/main/java/com/agentscustomerstickets/identity/web/AuthController.java`](src/main/java/com/agentscustomerstickets/identity/web/AuthController.java)
    - [`src/main/java/com/agentscustomerstickets/customer/web/AgentCustomersController.java`](src/main/java/com/agentscustomerstickets/customers/web/AgentCustomersController.java)
    - [`src/main/java/com/agentscustomerstickets/identity/web/MeController.java`](src/main/java/com/agentscustomerstickets/identity/web/MeController.java)

- **Return correct HTTP statuses (200/400/401/403/404/409)**
  - Central exception-to-status mapping:
    - [`src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java`](src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java)
  - Custom exceptions:
    - [`src/main/java/com/agentscustomerstickets/shared/error/ResourceNotFoundException.java`](src/main/java/com/agentscustomerstickets/shared/error/ResourceNotFoundException.java)
    - [`src/main/java/com/agentscustomerstickets/shared/error/ConflictException.java`](src/main/java/com/agentscustomerstickets/shared/error/ConflictException.java)
  - Authentication/authorization (401/403):
    - [`src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java`](src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java)

- **Human-readable error responses**
  - Error response model:
    - [`src/main/java/com/agentscustomerstickets/shared/error/ApiErrorResponse.java`](src/main/java/com/agentscustomerstickets/shared/error/ApiErrorResponse.java)
  - Error handling:
    - [`src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java`](src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java)

### Unit testing

- Unit tests to **some services** : [`src/test/java/com/agentscustomerstickets/customer/application/CustomerServiceTest.java`](src/test/java/com/agentscustomerstickets/customers/application/CustomerServiceTest.java)

- At least 1 **security-aware** unit test : `meRequiresAuthentication` in [`src/test/java/com/agentscustomerstickets/SecurityIntegrationTest.java`](src/test/java/com/agentscustomerstickets/SecurityIntegrationTest.java)

### Deliverables

- **[`README.md`](README.md)** (the current file) describing the project and how to build and run
- Basic **[`Dockerfile`](Dockerfile)** packaging the service
