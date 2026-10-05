# Web Chat

A new web chat project implemented from the OpenSpec contract in this repository. It is independent from the existing `chat` project.

## Stack

- Kotlin + Ktor backend, organized as a modular monolith
- PostgreSQL (persistence setup is the next foundation task)
- TypeScript + Vite + Web Components frontend
- WebSocket realtime transport

## Backend: local development

Requires JDK 21. Use the Gradle wrapper once it is generated, or run Gradle 9.7+ from the repository root:

```sh
gradle :backend:run
```

The server listens on `http://localhost:8080` (override with `PORT`).

- `GET /health` — health probe
- `POST /api/auth/guest` with `{"displayName":"Fox"}` — create an anonymous guest identity

Run the backend tests with:

```sh
gradle :backend:test
```

## Frontend

The `frontend/` workspace will contain the Vite/Web Components application. The initial client will be bootstrapped after persistence and API contracts are established.

## Specification workflow

`openspec/project.md` and `openspec/changes/initial-web-chat/` define the project architecture, behavior and implementation sequence. Update those artifacts when implementation decisions materially change the contract.
