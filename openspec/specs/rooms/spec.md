# Rooms

## Requirements

### Requirement: Public rooms are available
The system SHALL support public rooms that authenticated users can discover and join according to room policy.

#### Scenario: User joins public room
- GIVEN a public room exists
- WHEN an authorized user requests to join it
- THEN the user becomes a room member

### Requirement: Password-protected rooms are supported
The system SHALL support rooms protected by a password chosen by the room owner.

#### Scenario: Correct room password is supplied
- GIVEN a room requires a password
- WHEN a user supplies the correct password
- THEN the system permits the user to join according to the room policy

### Requirement: Room creators can appoint moderators
The system SHALL allow the room creator to appoint moderators for that room.

#### Scenario: Creator appoints moderator
- GIVEN the requester is the room creator
- WHEN the creator appoints an eligible user as moderator
- THEN that user receives moderator permissions for the room
