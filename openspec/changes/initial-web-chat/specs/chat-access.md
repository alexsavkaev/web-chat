# Chat Access

## Requirements

- The system MUST evaluate whether an authenticated user may access a chat resource.
- Access decisions MUST be expressed through a module contract rather than direct access to another module's persistence.
- Unauthorized access MUST fail consistently at the application boundary.
