# Design

## Architectural principle

**Modular Monolith First → Extract Services When Needed.** The backend SHALL be one deployable Ktor application. Domain capabilities are isolated into modules with explicit interfaces/events so new features can normally be added without changing unrelated modules.

## Repository layout

```text
web-chat/
├── openspec/
├── backend/
│   ├── auth/
│   ├── users/
│   ├── rooms/
│   ├── messages/
│   ├── direct-messages/
│   ├── media/
│   ├── presence/
│   ├── shared/
│   │   ├── database/
│   │   ├── security/
│   │   ├── events/
│   │   ├── errors/
│   │   └── common/
│   └── infrastructure/
│       ├── postgres/
│       ├── websocket/
│       └── storage/
└── frontend/
```

## Backend modules

- `auth`: authentication, sessions and authorization primitives.
- `users`: registered-user profile and identity data.
- `rooms`: room lifecycle, membership and room policy.
- `messages`: room message lifecycle and persistence.
- `direct-messages`: user-to-user private conversations.
- `media`: image upload/storage metadata and access control.
- `presence`: online-only presence state and realtime events.

Shared infrastructure provides technical primitives but MUST NOT become a dumping ground for domain logic.

## Module boundaries

A module owns its domain rules and persistence. Other modules consume its public application contracts or domain events rather than reaching directly into its tables or internal classes.

Cross-module communication should prefer explicit commands/queries and events. Cyclic dependencies are prohibited; if a dependency becomes architectural pressure, introduce a stable contract or event rather than coupling modules together.

## Persistence

PostgreSQL is the system of record. Database ownership follows module boundaries. Migrations are kept versioned and deterministic. Cross-module reads must go through application contracts unless a deliberately shared read model is introduced.

## Realtime

Ktor WebSocket infrastructure manages connections. Domain modules publish events; the realtime adapter maps authorized events to client protocol messages. Connection state is not domain state.

## Frontend

The frontend uses TypeScript, Vite and Web Components. Transport, client state and presentation components remain separated. The client consumes stable HTTP/WebSocket contracts and does not depend on backend implementation classes.

## Security

Authentication and authorization are enforced server-side. Room roles, private-message visibility and media access are evaluated at the application boundary. A client request MUST NOT be trusted merely because the UI hides an action.

## Evolution

Start with a modular monolith. A module may later be extracted into a service when operational or scaling needs justify it; explicit contracts and events are the mechanism that makes such extraction possible.
