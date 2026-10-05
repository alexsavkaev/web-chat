# Design

## Architecture

Use a modular monolith with one Ktor deployable. Each domain capability owns its application logic and persistence boundary and exposes explicit interfaces/events to other modules.

## Backend modules

1. `user-auth` — identity, authentication, sessions and authorization primitives.
2. `chat-access` — access decisions for chat resources.
3. `chat-rooms` — room lifecycle and membership.
4. `room-moderation` — moderation actions and room policy enforcement.
5. `chat-messages` — message creation, validation, persistence and retrieval.
6. `public-private-messages` — visibility and delivery rules for room messages.
7. `direct-messages` — one-to-one conversations and delivery.
8. `online-users` — presence state and realtime presence events.

## Persistence

PostgreSQL is the system of record. Tables should be owned by their module; cross-module access happens through contracts rather than direct table coupling.

## Realtime

WebSocket connections are managed by the Ktor application. Domain events are translated into client-facing realtime events. Connection/session concerns remain separate from message and domain logic.

## Frontend

Use TypeScript, Vite and Web Components. Keep transport, state, and UI components separated so the UI can evolve without coupling to Ktor internals.

## Testing

Each module gets unit tests for domain rules and integration tests for persistence/API behavior. Realtime contracts should have focused protocol tests.

## Evolution

Adding a feature should normally mean adding or extending a module and its contracts, not introducing dependencies between unrelated modules. Shared primitives must remain small and stable.
