# Handoffs

Handoffs preserve context between project phases, conversations, or CLI
executions.

They are useful when one step ends and another begins, especially when a
different agent, session, or operator needs to continue from the current state.

Handoffs must be objective and concise. They should summarize the current state,
important decisions, known risks, pending work, and the recommended next action.

A handoff does not replace a report:

```text
Report = detailed fact record of a completed execution.
Handoff = condensed context for continuation.
```

Real or temporary execution reports must be written to
`.harness/reports/runs/` and remain ignored by Git. Versioned report conventions
and templates stay under `.harness/reports/`.

Handoffs may be versioned when they represent stable project context. Temporary
handoffs should be handled carefully to avoid repository noise.
