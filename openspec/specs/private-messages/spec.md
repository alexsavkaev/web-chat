# Private Messages

## Requirements

### Requirement: Direct private conversations are supported
The system SHALL support private conversations between users with explicit participants.

#### Scenario: User starts a direct conversation
- GIVEN two users are eligible to communicate
- WHEN one user starts a direct conversation
- THEN the system creates or reuses the private conversation for those participants

### Requirement: Two private-message modes are supported
The system SHALL support the two private-message modes defined by the product contract: direct user-to-user conversations and private room messages restricted by room access.

#### Scenario: Private room message is sent
- GIVEN a user has access to a room
- WHEN the user sends a private message within that room
- THEN only the intended eligible recipients receive it

### Requirement: Private messages follow delivery and read state
The system SHALL apply delivery and read-state tracking to private messages.

#### Scenario: Private message is read
- GIVEN a private message has been delivered to its recipient
- WHEN the recipient opens the conversation
- THEN the system records the message as read
