# Phase 4.1.2 Canonical Decision JSON Report

## Summary

Phase 4.1.2 stabilizes the textual format of `decisions.json`, the primary
Decision Trace artifact. The semantic Decision Trace contract remains
unchanged.

## Motivation

Phase 4.1.1 left `decisions.md` and `decisions.mmd` matching expected fixtures
exactly, while `decisions.json` matched semantically but differed in raw text
because of Jackson's default pretty printer. This phase makes the JSON artifact
stable enough for pull request review, fixture tests, and agent consumption.

## What changed

- `DecisionTraceJsonWriter` now serializes through an explicit canonical
  Jackson pretty printer.
- JSON output is written as UTF-8 text with a guaranteed final newline.
- Fixture coverage now compares generated `decisions.json`,
  `decisions.md`, and `decisions.mmd` against expected artifacts as raw text.
- Writer coverage verifies final newline behavior and deterministic output from
  two consecutive writes of the same `DecisionTrace`.
- Phase 4 documentation now describes canonical JSON formatting as an output
  writer responsibility.

## JSON formatting policy

- Use two-space indentation.
- Use `": "` between object field names and values.
- Preserve record/model field order for Decision Trace records.
- Sort map entries by key if maps are introduced.
- Render empty arrays as `[]`.
- End the file with one newline.
- Keep the policy isolated to `com.codeatlas.output.decision.json`.
- Keep `com.codeatlas.core.decision` independent from Jackson and output
  concerns.

## Fixture compatibility

The existing
`examples/phase-4-decision-trace/01-if-throw-validation/expected/decisions.json`
fixture already matched the canonical format, so no semantic or textual fixture
contract change was required. Generated `decisions.json`, `decisions.md`, and
`decisions.mmd` now match the expected fixture files exactly.

## Tests executed

- `./gradlew test --tests com.codeatlas.output.decision.json.DecisionTraceJsonWriterTest --tests com.codeatlas.examples.DecisionTraceFixtureContractTest`
- `./gradlew test`
- `./gradlew build`
- `./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"`
- `cmp -s` for generated vs expected `decisions.json`
- `cmp -s` for generated vs expected `decisions.md`
- `cmp -s` for generated vs expected `decisions.mmd`
- `./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder --output build/phase-4-1-2-flow-check"`

## Test results

All executed tests and validation commands passed. The generated
`decisions.json` ends with a newline and matches the expected fixture exactly.
The generated Markdown and Mermaid decision artifacts also match exactly.
`analyze-flow` continues to generate `flow.json`, `flow.md`, `flow.mmd`,
`context-pack.md`, and `agent-handoff.md`.

## Files changed

- `src/main/java/com/codeatlas/output/decision/json/DecisionTraceJsonWriter.java`
- `src/test/java/com/codeatlas/output/decision/json/DecisionTraceJsonWriterTest.java`
- `src/test/java/com/codeatlas/examples/DecisionTraceFixtureContractTest.java`
- `docs/phase-4-decision-trace.md`
- `docs/phase-4-decision-trace-contract.md`
- `PHASE_4_1_2_CANONICAL_DECISION_JSON_REPORT.md`

## Known risks

- The writer relies on Jackson's record serialization preserving record
  component order, which matches the current Decision Trace model and is covered
  by exact fixture tests.
- Future extractors must keep decision and unresolved lists deterministic before
  handing the trace to the writer.

## Suggested next phase

Phase 4.2 - Java Decision Unresolved + Early Return.
