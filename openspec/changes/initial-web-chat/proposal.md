# Initial Chat

## Problem

The project needs an implementation-ready specification for a web chat whose core behavior is defined before coding and can evolve without breaking unrelated capabilities.

## Proposal

Build the first release as a modular monolith: one deployable Ktor application with explicit domain-module boundaries, contracts and events. Use PostgreSQL as the system of record, WebSocket for realtime delivery, and a TypeScript/Vite/Web Components frontend.

Start with baseline specifications for users, roles and permissions, rooms, messaging, private messages, and media. Refine the specifications during implementation as concrete edge cases and contracts are discovered.

## Initial scope

- Guest and registered-user access
- User identity, roles and permissions
- Public rooms
- Password-protected rooms
- Room creators appointing moderators
- Room messages
- Image attachments
- Two private-message modes: direct user-to-user conversations and private room messages
- Message delivery and read state
- Online-only user presence/list

## Architecture rule

**Modular Monolith First → Extract Services When Needed.** A new capability should normally be implemented as a new or extended module without changing unrelated modules. Service extraction is a later operational decision, enabled by explicit contracts/events.

## Out of scope for the initial change

Multi-region deployment, premature service decomposition, and unrelated platform features are deferred until the core chat workflow is working and tested.
