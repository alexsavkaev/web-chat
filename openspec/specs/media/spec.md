# Media

## Requirements

### Requirement: Image messages are supported
The system SHALL support attaching images to chat messages through the media subsystem.

#### Scenario: User attaches an image
- GIVEN a user is allowed to send a message
- WHEN the user attaches a supported image
- THEN the system stores the media metadata and makes the image reference available to authorized recipients

### Requirement: Media access follows message access
The system SHALL NOT expose a private image attachment to users who cannot access the associated message or conversation.

#### Scenario: Unauthorized user requests private image
- GIVEN an image belongs to a private message
- WHEN an unauthorized user requests the image
- THEN the system denies access
