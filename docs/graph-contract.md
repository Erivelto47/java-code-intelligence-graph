# Graph Contract

The active Phase 1 graph contract is maintained in
[`docs/phase-1-flow-graph-contract.md`](phase-1-flow-graph-contract.md).

In short:

- `flow.json` is the primary deterministic artifact.
- `flow.md`, `flow.mmd`, `context-pack.md`, and `agent-handoff.md` are derived
  from the same `FlowGraph`.
- Core packages must not import IntelliJ PSI or adapters.
- AI interpretations must not be mixed into deterministic graph facts.
