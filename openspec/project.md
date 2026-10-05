# web-chat

## Project overview

A web chat platform specified and implemented through OpenSpec. The repository is the implementation workspace; the OpenSpec artifacts are the source of truth for behavior and architecture.

## Architecture principle

**Modular Monolith First → Extract Services When Needed.** The backend is one deployable Ktor application. New capabilities are isolated into modules with explicit interfaces/events and should normally be addable without changing unrelated modules.

## Technology baseline

- Kotlin + Ktor backend
- PostgreSQL persistence
- TypeScript + Vite + Web Components frontend
- WebSocket for realtime communication
- OpenSpec CLI/workflow from Fission-AI

## Backend module baseline

- `auth`
- `users`
- `rooms`
- `messages`
- `direct-messages`
- `media`
- `presence`
- shared technical modules for `database`, `security`, `events`, `errors`, and `common`
- infrastructure adapters for `postgres`, `websocket`, and `storage`

## Development principles

- Keep module boundaries explicit.
- Define behavior and contracts before implementation details.
- A module owns its domain rules and persistence boundary.
- Prefer explicit application contracts and events for cross-module communication.
- Avoid cyclic dependencies and direct cross-module table access.
- Cover module behavior with automated tests.
- Prefer additive changes over cross-module coupling.
- Refine baseline specs during implementation when concrete edge cases are discovered.
