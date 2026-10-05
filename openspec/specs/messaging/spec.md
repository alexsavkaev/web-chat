# Messaging

## Requirements

### Requirement: Messages are persisted
The system SHALL persist chat messages with stable identifiers, author identity, conversation context, and creation time.

#### Scenario: User sends a message
- GIVEN a user has access to a conversation
- WHEN the user sends a valid message
- THEN the system persists the message and makes it available to authorized participants

### Requirement: Message delivery is realtime
The system SHALL deliver new messages to connected eligible recipients through the realtime channel.

#### Scenario: Connected recipient receives a new message
- GIVEN an eligible recipient has an active realtime connection
- WHEN a new message is accepted
- THEN the recipient receives the message event without requiring a page refresh

### Requirement: Read state is tracked
The system SHALL support delivery and read state for message flows where read tracking is enabled.

#### Scenario: Recipient reads a message
- GIVEN a delivered message is unread
- WHEN the recipient reads it
- THEN the system records the read state
