# Initial Web Chat

## Problem

The project needs a concrete, implementation-ready foundation for a modular web chat with authentication, rooms, moderation, messages, direct messages, and online presence.

## Proposal

Implement the first web-chat capability set as a modular monolith on Ktor with PostgreSQL persistence and a TypeScript/Vite/Web Components client. Use WebSocket for realtime delivery. Keep domain modules independently testable and communicate through explicit contracts/events.

## Scope

- User authentication and access control
- Chat rooms and room access
- Room moderation
- Chat messages
- Public/private messages
- Direct messages
- Online users/presence

## Out of scope

Advanced scaling, multi-region deployment, and unrelated platform features are deferred until the core chat workflow is operational.
