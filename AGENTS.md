# AGENTS.md

## Task execution and blocking

Tasks must not be treated as blocked merely because of how the task was originally created or represented.

A task is blocked only when there is a real, current dependency or external constraint that prevents progress. The source or creation mechanism of the task is not, by itself, a valid reason to mark the task as blocked.

When evaluating whether work can proceed:
- distinguish the task's origin from its actual dependencies;
- do not infer a blocker from task metadata alone;
- identify and document the concrete dependency or constraint that prevents progress;
- if the supposed blocker is only an artifact of how the task was created, continue with the task instead of marking it blocked.
