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

The auth module exposes `GuestIdentityRepository` as its persistence boundary. The current `InMemoryGuestIdentityRepository` is deliberately a temporary adapter for the bootstrap slice; PostgreSQL persistence must be introduced behind this contract in the foundation work.

The `frontend/` workspace will contain the Vite/Web Components application. The initial client will be bootstrapped after persistence and API contracts are established.

## Specification workflow

`openspec/project.md` and `openspec/changes/initial-web-chat/` define the project architecture, behavior and implementation sequence. Update those artifacts when implementation decisions materially change the contract.
