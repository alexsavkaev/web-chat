# Users

## Requirements

### Requirement: Guests can access the public chat surface
The system SHALL allow an unauthenticated guest to enter the public chat surface without creating a registered account.

#### Scenario: Guest enters public chat
- GIVEN a guest has no authenticated account
- WHEN the guest opens the public chat
- THEN the system allows access to the public chat surface

### Requirement: Registered users have persistent identity
The system SHALL provide registered users with a stable identity used by chat, moderation, private messaging, and presence features.

#### Scenario: Registered user authenticates
- GIVEN a registered user provides valid authentication credentials
- WHEN authentication succeeds
- THEN the system establishes an authenticated session for that user

### Requirement: Roles are explicit
The system SHALL represent the roles needed by the chat domain and SHALL enforce role-based permissions at the application boundary.

#### Scenario: Moderator permission is checked
- GIVEN a user has moderator privileges for a room
- WHEN the user performs a moderation action
- THEN the action is authorized according to the room's moderation policy
