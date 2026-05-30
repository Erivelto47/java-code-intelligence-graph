# Agent Handoff

## Repository

local/java-code-intelligence-graph

## Project Path

`examples/java-simple`

## Entrypoint

`com.company.FooService.processOrder`

## Flow Path

`.code-atlas/flows/com/company/FooService/processOrder`

## Source Files

- `src/main/java/com/company/FooService.java`

## Generated Artifacts

- `flow.json` - `.code-atlas/flows/com/company/FooService/processOrder/flow.json`
- `flow.md` - `.code-atlas/flows/com/company/FooService/processOrder/flow.md`
- `flow.mmd` - `.code-atlas/flows/com/company/FooService/processOrder/flow.mmd`
- `context-pack.md` - `.code-atlas/flows/com/company/FooService/processOrder/context-pack.md`
- `agent-handoff.md` - `.code-atlas/flows/com/company/FooService/processOrder/agent-handoff.md`

## Graph Summary

- Schema version: `1.0`
- Node count: `6`
- Edge count: `5`
- Analyzer: `source-text-flow-analyzer`
- Deterministic: `true`

## Detected Nodes

- `class:com.company.FooService` CLASS `com.company.FooService`
- `method:com.company.FooService.processOrder` METHOD `com.company.FooService.processOrder`
- `method:com.company.FooService.validate` METHOD `com.company.FooService.validate`
- `method:repository.save` METHOD `repository.save`
- `method:paymentClient.charge` METHOD `paymentClient.charge`
- `method:com.company.FooService.mapper` METHOD `com.company.FooService.mapper`

## Detected Edges

- `DECLARES` `class:com.company.FooService` -> `method:com.company.FooService.processOrder`
- `CALLS` `method:com.company.FooService.processOrder` -> `method:com.company.FooService.validate`
- `CALLS` `method:com.company.FooService.processOrder` -> `method:repository.save`
- `CALLS` `method:com.company.FooService.processOrder` -> `method:paymentClient.charge`
- `CALLS` `method:com.company.FooService.processOrder` -> `method:com.company.FooService.mapper`

## Commands

```bash
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

## Notes

This handoff is intended for agents that can read files by exact path but cannot freely traverse repository directories.
