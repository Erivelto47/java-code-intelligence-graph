# Graph Contract

This document is the stable entry point for the graph contract documentation.

The active Phase 1 graph contract is maintained in:
[docs/phase-1-flow-graph-contract.md](phase-1-flow-graph-contract.md)

Phase 1 contract summary:

- `flow.json` is the primary deterministic artifact.
- `flow.md`, `flow.mmd`, `context-pack.md`, and `agent-handoff.md` are derived from the same `FlowGraph`.
- `resolutions`, `boundaries`, and `unresolved` are part of the deterministic graph model.
- Core packages must not import IntelliJ PSI or adapters.
- PSI belongs only in adapter code.
- AI interpretations must not be mixed into deterministic graph facts.