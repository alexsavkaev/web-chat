# Web Chat — Product Vision

## Product Vision
Web Chat is a small but modern independent web chat platform. It should support public rooms, private messaging, users, roles, moderation, and reputation while remaining simple enough to develop incrementally.

The product should feel like a real, usable modern chat—not a proof of concept or a collection of disconnected APIs.

## MVP Goal
A user should be able to:
- register and log in, or enter as a guest;
- see the main room and other available rooms;
- create rooms;
- enter a room and see its users;
- send and receive messages in real time;
- send private messages to other users;
- see basic user reputation/karma;
- use capabilities appropriate to their role;
- experience basic moderation by moderators/admins.

## Core Features
### Identity and Access
- Registration and login.
- Guest access without registration.
- User identity and basic profile information.
- Clear distinction between authenticated users and guests.

### Rooms
- Main room available by default.
- Multiple rooms.
- Room list.
- Ability to create rooms.
- Enter and leave rooms.
- Basic room membership/state.

### Users
- User list.
- Basic user profile information.
- Presence/state where useful for the MVP.

### Messaging
- Real-time public messages in rooms.
- Message history.
- Private messages between users.
- Basic message lifecycle and moderation support.

### Roles and Permissions
The MVP has three basic roles: user, moderator, admin.
Roles should provide clear differences in available capabilities without introducing an unnecessarily complex permission system.

### Moderation
Moderators/admins should have basic tools such as deleting messages, muting/restricting users, kicking users where appropriate, banning users where appropriate, and managing roles where appropriate.
Moderation should be understandable, predictable, and sufficient for a small community.

### Karma / Reputation
The product includes a simple reputation mechanism.
The MVP should define what karma represents, how karma can change, where it is displayed, and a simple transparent mechanism rather than a complex gamification system.

### Realtime
Realtime communication is a core part of the product.
WebSocket-based communication should provide timely delivery of relevant chat and presence events without requiring manual page refreshes.

### Frontend
The frontend should provide at least authentication/guest entry, room list, current room, message list, message input, user list, private messaging, basic profile information, and relevant moderation controls for privileged users.
The UI should be small, clear, and usable rather than overloaded with features.

### Backend
The backend should provide the product capabilities through a clean application/domain structure.
The implementation should keep sensible boundaries between HTTP/API handling, realtime communication, application/domain logic, persistence, infrastructure, identity/authentication, and authorization/moderation.
Exact classes, endpoints, package structure, protocols, and internal abstractions are implementation decisions for the engineering team.

## Persistence
PostgreSQL is the persistence layer for the MVP.
Liquibase is used for database schema migrations.
Persistent data should cover users/identities, rooms, memberships, messages, private conversations/messages, roles/permissions, moderation state, and karma/reputation.
The database design should remain intentionally simple and evolve with the product.

## Quality Bar
The MVP is not considered complete merely because the application starts.
The project should include reproducible backend builds, reproducible frontend builds, unit tests, integration tests where appropriate, realtime/WebSocket tests, database/migration tests, linters, formatters, static analysis, GitHub Actions CI, and a development workflow that can reliably reproduce the expected environment.
CI should act as a quality gate for changes.

## Out of Scope for MVP
OAuth/social login; complex account management; file/image/video sharing; voice/video calls; reactions; threads; complex notification systems; native mobile applications; federation; end-to-end encryption; microservices/Kubernetes architecture; distributed scaling; advanced observability infrastructure; complex gamification; highly granular enterprise-style RBAC.
These may be considered later if the product requires them.

## Product Principles
1. User-first: prioritize a coherent usable chat experience.
2. Simple implementation: prefer the simplest design that satisfies the product requirement.
3. Incremental delivery: build the product in small, independently verifiable steps.
4. Testability: important behavior must be easy to test.
5. Maintainability: avoid unnecessary coupling and premature abstraction.
6. Product over technology: technology choices should serve the product rather than become the product.

## Guidance for Agora
product.md is the product-level source of truth.
Agora and its autonomous workers should use it to decide what the team is building toward, but should not treat it as a prescribed implementation.
For each proposed feature or technical task, the team should consider: Is this required for the MVP? Which user scenario does it improve? Is there a simpler way to satisfy that scenario? Is the complexity justified now? What tests prove that the behavior works? Does the change keep the product coherent?
Non-MVP ideas should normally be deferred unless they are necessary to support an MVP requirement.
The engineering team is responsible for deciding implementation details: architecture, APIs, data structures, packages, classes, infrastructure, and internal abstractions.

## MVP Definition of Done
The MVP is reached when a user can register/login or enter as a guest; see the main room and other rooms; create and enter rooms; see users in a room; send and receive messages in real time; use private messaging; see/use the basic reputation system; and experience basic moderation according to their role.
The project must also have reproducible backend/frontend builds, database migrations, meaningful automated tests, code quality tooling, and a working GitHub Actions quality gate.

## North Star
Web Chat should feel like a small but real modern chat product.
It should not feel like a proof of concept, a technical demo, a collection of APIs, or an unfinished framework.
It should feel like a coherent MVP that can be used, tested, reviewed, and extended.
