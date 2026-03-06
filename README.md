# Agents Customers Tickets

Spring Boot 3.x (Java 21) backend service implemented as a package-by-feature monolith (clear internal modules, single deployable).

## Prerequisites

- Java 21
- Docker (optional, for MySQL)

## Deployment

With [docker-compose.yml](docker-compose.yml):

```bash
# Start MySQL and run the application service
docker compose up -d
```

The application service listens on `http://localhost:8080`.

## Development

```bash
# Start only MySQL
docker compose up -d mysql

# Build the application
./scripts/build.sh # (--help).

# Run the application service
java -jar target/agents-customers-tickets-0.0.1-SNAPSHOT.jar
```

## Smoke test

In another terminal, you can run end-to-end smoke test:

```bash
./scripts/smoke-test.sh
```

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

## Scripts

Scripts are available at repo root and are intended to be run from WSL.

- `./scripts/build.sh` (builds the application and runs unit/integration tests)
- `./scripts/deploy.sh`
- `./scripts/smoke-test.sh` (end-to-end smoke test; run while the service is up)
- `./scripts/undeploy.sh`
- `./scripts/clean.sh`

## Exercise requirements mapping

### General

- **Spring Boot 3.x**
  - [`pom.xml`](pom.xml)
  - [`src/main/java/com/agentscustomerstickets/Application.java`](src/main/java/com/agentscustomerstickets/Application.java)

- **Spring Data JPA (Java Persistence API) over MySQL**:
  - Entities / repositories:
    - [`src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserEntity.java)
    - [`src/main/java/com/agentscustomerstickets/identity/infra/UserRepository.java`](src/main/java/com/agentscustomerstickets/identity/infra/UserRepository.java)
    - [`src/main/java/com/agentscustomerstickets/tickets/infra/TicketEntity.java`](src/main/java/com/agentscustomerstickets/tickets/infra/TicketEntity.java)
    - [`src/main/java/com/agentscustomerstickets/tickets/infra/TicketRepository.java`](src/main/java/com/agentscustomerstickets/tickets/infra/TicketRepository.java)
  - MySQL configuration:
    - [`src/main/resources/application.yml`](src/main/resources/application.yml)
    - [`docker-compose.yml`](docker-compose.yml)

- **REST API via Spring MVC** with **Spring Validation**
  | Method | Endpoint | Access / Notes |
  | --- | --- | --- |
  | `POST` | `/api/auth/token` | Public login endpoint. Accepts username/password and returns JWT `access_token`. |
  | `GET`, `PUT` | `/api/me` | Requires valid `Authorization: Bearer <token>`. Read/update current authenticated user profile. |
  | `POST` | `/api/agents` | `ADMIN` only. Creates a new agent user/account. |
  | `GET` | `/api/agents` | `ADMIN` only. Lists all agent accounts. |
  | `POST` | `/api/customers` | `AGENT` can create own customers; `ADMIN` can create customers when `agentId` is provided. |
  | `GET` | `/api/customers` | `AGENT` sees customers assigned to that agent; `ADMIN` can list all or filter by `agentId`. |
  | `POST` | `/api/tickets` | `CUSTOMER` only. Creates a ticket tied to the authenticated customer identity. |
  | `GET` | `/api/tickets` | Role-based result set: `CUSTOMER` sees own tickets, `AGENT` sees assigned tickets, `ADMIN` can query globally and filter by `agentId` and/or `customerId`. |
  - Request DTO annotations are used across controllers, e.g.:
    - [`createCustomer`](src/main/java/com/agentscustomerstickets/customers/web/CustomersController.java#L60) in [`CustomersController`](src/main/java/com/agentscustomerstickets/customers/web/CustomersController.java)
    - [`CreateTicketRequest`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java#L53) in [`TicketsController`](src/main/java/com/agentscustomerstickets/tickets/web/TicketsController.java)
  - **Return correct HTTP statuses (200/400/401/403/404/409)** with **human-readable error responses**
    - Central exception-to-status mapping:
      - [`src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java`](src/main/java/com/agentscustomerstickets/shared/error/ApiExceptionHandler.java)
      - [`src/main/java/com/agentscustomerstickets/shared/error/ApiErrorResponse.java`](src/main/java/com/agentscustomerstickets/shared/error/ApiErrorResponse.java)
    - Custom exceptions:
      - [`src/main/java/com/agentscustomerstickets/shared/error/ResourceNotFoundException.java`](src/main/java/com/agentscustomerstickets/shared/error/ResourceNotFoundException.java)
      - [`src/main/java/com/agentscustomerstickets/shared/error/ConflictException.java`](src/main/java/com/agentscustomerstickets/shared/error/ConflictException.java)
    - Authentication/authorization (401/403):
      - [`src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java`](src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java)

### Role-Based Access Control (RBAC)

Users can only access resources appropriate to their role.

- Role enum: [`ADMIN`, `AGENT`, `CUSTOMER`](src/main/java/com/agentscustomerstickets/identity/domain/Role.java)
- **JWT Claims**: Roles are embedded in JWT tokens in [`issueAccessToken`](src/main/java/com/agentscustomerstickets/identity/infra/NimbusJwtService.java#L27) and mapped to Spring Security authorities in [`jwtAuthConverter`](src/main/java/com/agentscustomerstickets/identity/infra/SecurityConfig.java#L70).
- Example of method-level security, using `@PreAuthorize` annotations:
  [`src/main/java/com/agentscustomerstickets/agents/web/AgentsController.java`](src/main/java/com/agentscustomerstickets/agents/web/AgentsController.java)
- Username/password authentication and JWT security flow:
  - Token issuance endpoint:
    - `POST /api/auth/token`
    - [`src/main/java/com/agentscustomerstickets/identity/web/AuthController.java`](src/main/java/com/agentscustomerstickets/identity/web/AuthController.java)
  - Authentication logic:
    - [`src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java`](src/main/java/com/agentscustomerstickets/identity/application/IdentityService.java)

### Unit testing

- Unit tests to **some services** : [`src/test/java/com/agentscustomerstickets/customer/application/CustomerServiceTest.java`](src/test/java/com/agentscustomerstickets/customers/application/CustomerServiceTest.java)

- At least 1 **security-aware** unit test : `meRequiresAuthentication` in [`src/test/java/com/agentscustomerstickets/SecurityIntegrationTest.java`](src/test/java/com/agentscustomerstickets/SecurityIntegrationTest.java)

### Deliverables

- **[`README.md`](README.md)** (the current file) describing the project and how to build and run
- **[`Dockerfile`](Dockerfile)** for the application service
- **[`docker-compose.yml`](docker-compose.yml)** for local orchestration (MySQL + application service)
