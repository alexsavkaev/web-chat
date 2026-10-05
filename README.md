# Web Chat

A new web chat project implemented from the OpenSpec contract in this repository. It is independent from the existing `chat` project.

## Stack

- Kotlin + Ktor backend, organized as a modular monolith
- PostgreSQL (persistence setup is the next foundation task)
- TypeScript + Vite + Web Components frontend
- WebSocket realtime transport will be added with the realtime implementation

## Backend: local development

Requires JDK 21. The repository includes a Gradle Wrapper for reproducible builds:

```sh
./gradlew :backend:run
```

On Windows:

```bat
gradlew.bat :backend:run
```

The server listens on `http://localhost:8080` (override with `PORT`).

- `GET /health` — health probe
- `POST /api/auth/guest` with `{"displayName":"Fox"}` — create an anonymous guest identity

Run the backend tests with:

```sh
./gradlew :backend:test
```

## Architecture notes

- PostgreSQL persistence uses Flyway migrations in `backend/src/main/resources/db/migration`.
- Configure the connection with `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`; defaults target local `webchat` DB.
- Guest identities are persisted through auth's `GuestIdentityRepository`; `InMemoryGuestIdentityRepository` is an explicit temporary/test adapter only.
- `./gradlew :backend:test` runs the fast tests. `./gradlew :backend:integrationTest` requires PostgreSQL; configure credentials with the environment variables above.

The `frontend/` workspace will contain the Vite/Web Components application. The initial client will be bootstrapped after persistence and API contracts are established.

## Specification workflow

`openspec/project.md` and `openspec/changes/initial-web-chat/` define the project architecture, behavior and implementation sequence. Update those artifacts when implementation decisions materially change the contract.
