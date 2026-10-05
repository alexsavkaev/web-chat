# web-chat

## Project overview

A web chat platform designed to be implemented from OpenSpec artifacts.

## Architecture

The backend is a modular monolith: one Ktor deployable containing independent modules with explicit contracts and events. New capabilities should be added as modules without creating hard dependencies between existing modules.

## Technology baseline

- Kotlin + Ktor backend
- PostgreSQL persistence
- TypeScript + Vite + Web Components frontend
- WebSocket for realtime communication

## Development principles

- Keep module boundaries explicit.
- Define contracts before implementation details.
- Cover module behavior with automated tests.
- Prefer additive changes over cross-module coupling.
- Treat the OpenSpec change artifacts as the implementation source of truth.
